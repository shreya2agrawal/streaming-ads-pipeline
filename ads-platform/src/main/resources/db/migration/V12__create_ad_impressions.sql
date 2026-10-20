-- One row per ad that was actually shown, reported by the player.
-- A decision only means an ad was chosen; this records that it appeared.
--
-- slot_token is the token issued for the slot at decision time
-- (the same token used in the click URL). It identifies exactly one
-- served ad, and the unique constraint keeps it to one impression.
CREATE TABLE ad_impressions (
    id            BIGSERIAL    PRIMARY KEY,
    slot_token    VARCHAR(64)  NOT NULL,
    decision_id   UUID         NOT NULL,
    campaign_id   BIGINT       NOT NULL,
    creative_id   BIGINT       NOT NULL,
    impressed_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_ad_impressions_slot_token
        UNIQUE (slot_token),

    CONSTRAINT fk_ad_impressions_slot
        FOREIGN KEY (slot_token)
        REFERENCES ad_decision_slots(click_token)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ad_impressions_decision
        FOREIGN KEY (decision_id)
        REFERENCES ad_decisions(decision_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ad_impressions_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE RESTRICT
);

-- Advertiser stats read impressions per campaign over a date range.
CREATE INDEX idx_ad_impressions_campaign_time
    ON ad_impressions(campaign_id, impressed_at);