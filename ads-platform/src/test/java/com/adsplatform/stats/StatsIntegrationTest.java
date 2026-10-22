package com.adsplatform.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import com.adsplatform.serving.click.ClickService;
import com.adsplatform.serving.conversion.ConversionService;
import com.adsplatform.serving.decision.AdDecision;
import com.adsplatform.serving.decision.AdDecisionRepository;
import com.adsplatform.serving.decision.DecisionOutcome;
import com.adsplatform.serving.decision.RankedAd;
import com.adsplatform.serving.impression.ImpressionService;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Runs the real stats SQL against PostgreSQL. Mocked tests cannot catch
 * a wrong column, a wrong range bound, or a fan-out join.
 *
 * Each run uses its own advertiser, so other data in the database cannot
 * affect the result, and removes everything it created afterwards.
 *
 * The data: 3 served ads, 3 impressions, 2 clicks ($3.00), and
 * conversions of 100.00 USD and 20.00 EUR (confirmed) plus 50.00 USD
 * (cancelled). A fan-out bug would report 6 impressions and 6 clicks.
 */
@SpringBootTest
class StatsIntegrationTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Autowired private StatsService statsService;
    @Autowired private ImpressionService impressionService;
    @Autowired private ClickService clickService;
    @Autowired private ConversionService conversionService;
    @Autowired private PlacementRepository placementRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CreativeRepository creativeRepository;
    @Autowired private AdDecisionRepository adDecisionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String advertiser;
    private Placement placement;
    private Campaign campaign;
    private Creative creative;

    @BeforeEach
    void createEvents() {
        setUpCampaign();

        String first = issueSlotToken();
        String second = issueSlotToken();
        String third = issueSlotToken();

        impressionService.recordImpression(first);
        impressionService.recordImpression(second);
        impressionService.recordImpression(third);

        clickService.recordClick(first);
        clickService.recordClick(second);

        Instant now = Instant.now();
        conversionService.recordConversion(advertiser, first, "B1", new BigDecimal("100.00"), "USD", now);
        conversionService.recordConversion(advertiser, first, "B3", new BigDecimal("20.00"), "EUR", now);
        conversionService.recordConversion(advertiser, second, "B2", new BigDecimal("50.00"), "USD", now);
        conversionService.cancelConversion(advertiser, second, "B2");
    }

    @AfterEach
    void cleanUp() {
        // Children before parents, as the foreign keys require.
        if (campaign != null) {
            jdbcTemplate.update("DELETE FROM ad_conversions WHERE campaign_id = ?", campaign.getId());
            jdbcTemplate.update("DELETE FROM ad_impressions WHERE campaign_id = ?", campaign.getId());
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

    // A day either side, so a run near midnight cannot fall outside the range.
    private StatsReport report(StatsGrouping grouping) {
        LocalDate today = LocalDate.now(NEW_YORK);
        return statsService.report(advertiser, grouping, today.minusDays(1), today.plusDays(1));
    }

    private static void assertExpectedTotals(StatsRow row) {
        assertThat(row.impressions()).isEqualTo(3);
        assertThat(row.clicks()).isEqualTo(2);
        assertThat(row.spend()).isEqualByComparingTo("3.00");
        assertThat(row.conversions()).isEqualTo(2);
        assertThat(row.cancelledConversions()).isEqualTo(1);
        assertThat(row.conversionValue())
                .containsEntry("USD", new BigDecimal("100.00"))
                .containsEntry("EUR", new BigDecimal("20.00"))
                .hasSize(2);
        assertThat(row.returnOnAdSpend()).isEqualByComparingTo("33.33");   // 100.00 / 3.00
    }

    @Test
    void byCampaignCountsEachEventOnce() {
        StatsReport report = report(StatsGrouping.CAMPAIGN);

        assertThat(report.rows()).hasSize(1);
        StatsRow row = report.rows().get(0);
        assertThat(row.key()).isEqualTo(campaign.getId().toString());
        assertThat(row.label()).isEqualTo(campaign.getName());
        assertExpectedTotals(row);
        assertThat(row.ctr()).isEqualByComparingTo("0.6667");   // 2 / 3
        assertExpectedTotals(report.totals());
    }

    @Test
    void byDayListsEveryDayAndTheDaysAddUpToTheTotals() {
        StatsReport report = report(StatsGrouping.DAY);

        // Yesterday, today and tomorrow, even the days without activity.
        assertThat(report.rows()).hasSize(3);
        assertThat(report.rows().stream().mapToLong(StatsRow::impressions).sum()).isEqualTo(3);
        assertThat(report.rows().stream().mapToLong(StatsRow::clicks).sum()).isEqualTo(2);
        assertExpectedTotals(report.totals());
    }

    @Test
    void byPlacementReachesThePlacementThroughEachDecision() {
        StatsReport report = report(StatsGrouping.PLACEMENT);

        StatsRow row = findRow(report.rows(), placement.getId().toString());
        assertExpectedTotals(row);
        assertExpectedTotals(report.totals());
    }

    @Test
    void byCreativeReachesConversionsThroughTheirClick() {
        StatsReport report = report(StatsGrouping.CREATIVE);

        StatsRow row = findRow(report.rows(), creative.getId().toString());
        assertExpectedTotals(row);
        assertExpectedTotals(report.totals());
    }

    private static StatsRow findRow(List<StatsRow> rows, String key) {
        return rows.stream()
                .filter(row -> key.equals(row.key()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No row with key " + key));
    }

    private void setUpCampaign() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        advertiser = "IT stats " + suffix;

        placement = placementRepository.save(Placement.create(
                "it-stats-" + suffix, PlacementType.MID_ROLL, 2, 30,
                Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)));

        campaign = campaignRepository.save(Campaign.builder()
                .name("IT stats campaign " + suffix)
                .advertiser(advertiser)
                .agency("Direct")
                .budget(new BigDecimal("1000.00"))
                .flightStart(LocalDate.now().minusDays(1))
                .flightEnd(LocalDate.now().plusDays(30))
                .status(Campaign.CampaignStatus.ACTIVE)
                .bidCpc(new BigDecimal("1.50"))
                .build());

        creative = creativeRepository.save(Creative.builder()
                .campaign(campaign)
                .name("IT stats creative " + suffix)
                .creativeType(CreativeType.VIDEO)
                .mediaUrl("https://cdn.example.com/it.mp4")
                .destinationUrl("https://advertiser.example.com/it")
                .contentText("Integration test ad")
                .durationSeconds(15)
                .status(CreativeStatus.ACTIVE)
                .safetyStatus(CreativeSafetyStatus.APPROVED)
                .build());
    }

    // A fresh decision with one slot for this campaign; returns its slot token.
    private String issueSlotToken() {
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
}