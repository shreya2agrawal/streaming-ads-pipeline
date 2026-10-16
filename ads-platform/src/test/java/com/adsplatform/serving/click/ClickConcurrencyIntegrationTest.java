package com.adsplatform.serving.click;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.placement.Placement;
import com.adsplatform.placement.PlacementRepository;
import com.adsplatform.placement.PlacementType;
import com.adsplatform.serving.click.ClickResult.Outcome;
import com.adsplatform.serving.decision.AdDecision;
import com.adsplatform.serving.decision.AdDecisionRepository;
import com.adsplatform.serving.decision.DecisionOutcome;
import com.adsplatform.serving.decision.RankedAd;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Runs against the real database: only real row locks and real
 * unique constraints can prove these guarantees. Mocks lock nothing.
 *
 * Not @Transactional on purpose: each thread needs its own committed
 * transaction, exactly as in production. Rows are cleaned up after.
 */
@SpringBootTest
class ClickConcurrencyIntegrationTest {

    @Autowired private ClickService clickService;
    @Autowired private PlacementRepository placementRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CreativeRepository creativeRepository;
    @Autowired private AdDecisionRepository adDecisionRepository;
    @Autowired private AdClickRepository adClickRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Placement placement;
    private Campaign campaign;
    private Creative creative;

    @AfterEach
    void cleanUp() {
        // Children before parents, as the foreign keys require.
        if (campaign != null) {
            jdbcTemplate.update("DELETE FROM ad_clicks WHERE campaign_id = ?", campaign.getId());
        }
        if (placement != null) {
            // Cascades to ad_decision_slots.
            jdbcTemplate.update("DELETE FROM ad_decisions WHERE placement_id = ?", placement.getId());
        }
        if (creative != null) {
            jdbcTemplate.update("DELETE FROM creatives WHERE id = ?", creative.getId());
        }
        if (campaign != null) {
            jdbcTemplate.update("DELETE FROM campaigns WHERE id = ?", campaign.getId());
        }
        if (placement != null) {
            jdbcTemplate.update("DELETE FROM placements WHERE id = ?", placement.getId());
        }
    }

    @Test
    void sameTokenClickedTwiceAtOnceIsChargedOnce() throws Exception {
        setUpCampaign("1000.00", "0.00");
        String token = issueClickToken();

        List<ClickResult> results = clickConcurrently(token, token);

        assertThat(results).extracting(ClickResult::outcome)
                .containsExactlyInAnyOrder(Outcome.CHARGED, Outcome.DUPLICATE);
        assertThat(adClickRepository.countByClickToken(token)).isEqualTo(1);
        assertThat(currentSpend()).isEqualByComparingTo("1.50");
    }

    @Test
    void twoClicksNearTheBudgetNeverOverspend() throws Exception {
        // $1.60 left; each click is priced at $1.50.
        setUpCampaign("1000.00", "998.40");
        String first = issueClickToken();
        String second = issueClickToken();

        List<ClickResult> results = clickConcurrently(first, second);

        assertThat(results).extracting(ClickResult::chargedAmount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(new BigDecimal("1.50"), new BigDecimal("0.10"));
        assertThat(currentSpend()).isEqualByComparingTo("1000.00");
    }

    // Both threads wait at the latch, then click at the same moment.
    private List<ClickResult> clickConcurrently(String firstToken, String secondToken)
            throws Exception {

        CountDownLatch startingGun = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Callable<ClickResult>> clicks = List.of(
                    () -> {
                        startingGun.await();
                        return clickService.recordClick(firstToken);
                    },
                    () -> {
                        startingGun.await();
                        return clickService.recordClick(secondToken);
                    });

            List<Future<ClickResult>> futures = new ArrayList<>();
            for (Callable<ClickResult> click : clicks) {
                futures.add(executor.submit(click));
            }

            startingGun.countDown();

            List<ClickResult> results = new ArrayList<>();
            for (Future<ClickResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;

        } finally {
            executor.shutdownNow();
        }
    }

    private void setUpCampaign(String budget, String spent) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        placement = Placement.create(
                "it-click-" + suffix, PlacementType.MID_ROLL, 2, 30,
                Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO));
        placement = placementRepository.save(placement);

        Campaign newCampaign = Campaign.builder()
                .name("IT click campaign " + suffix)
                .advertiser("Integration Test")
                .agency("Direct")
                .budget(new BigDecimal(budget))
                .flightStart(LocalDate.now().minusDays(1))
                .flightEnd(LocalDate.now().plusDays(30))
                .status(Campaign.CampaignStatus.ACTIVE)
                .bidCpc(new BigDecimal("1.50"))
                .build();
        newCampaign.setSpent(new BigDecimal(spent));
        campaign = campaignRepository.save(newCampaign);

        creative = creativeRepository.save(Creative.builder()
                .campaign(campaign)
                .name("IT creative " + suffix)
                .creativeType(CreativeType.VIDEO)
                .mediaUrl("https://cdn.example.com/it.mp4")
                .destinationUrl("https://advertiser.example.com/it")
                .contentText("Integration test ad")
                .durationSeconds(15)
                .status(CreativeStatus.ACTIVE)
                .safetyStatus(CreativeSafetyStatus.APPROVED)
                .build());
    }

    // A fresh decision with one slot for this campaign; returns its click token.
    private String issueClickToken() {
        AdRequestContext context = new AdRequestContext(
                "it-" + UUID.randomUUID(), placement.getId(), DeviceType.TV,
                "show-it", "viewer-it", Set.of(), 1, Instant.now());

        RankedAd ranked = new RankedAd(
                new CandidateAd(
                        new CandidateAd.CampaignSnapshot(
                                campaign.getId(), campaign.getName(), campaign.getStatus(),
                                campaign.getFlightStart(), campaign.getFlightEnd(),
                                null, null, null, null, null, null, null,
                                campaign.getBidCpc()),
                        new CandidateAd.CreativeSnapshot(
                                creative.getId(), CreativeType.VIDEO, CreativeStatus.ACTIVE,
                                CreativeSafetyStatus.APPROVED, 15),
                        new CandidateAd.PlacementSnapshot(
                                placement.getId(), true, Set.of(DeviceType.TV),
                                Set.of(CreativeType.VIDEO), 30)),
                0.02,
                0.03);

        AdDecision decision = AdDecision.start(
                UUID.randomUUID(),
                context,
                new DecisionOutcome(List.of(ranked), List.of(), 1, null),
                Instant.now());

        String token = "it-" + UUID.randomUUID();
        decision.addSlot(ranked, token);
        adDecisionRepository.save(decision);

        return token;
    }

    private BigDecimal currentSpend() {
        return campaignRepository.findById(campaign.getId()).orElseThrow().getSpent();
    }
}