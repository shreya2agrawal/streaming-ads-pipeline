-- One row per ad decision request, including no-fills,
-- so fill rate can be measured.
CREATE TABLE ad_decisions (
    decision_id      UUID         PRIMARY KEY,
    request_id       VARCHAR(100) NOT NULL,
    placement_id     BIGINT       NOT NULL,
    device_type      VARCHAR(50)  NOT NULL,
    show_id          VARCHAR(255) NOT NULL,
    viewer_track_id  VARCHAR(255),
    requested_slots  INTEGER      NOT NULL CHECK (requested_slots > 0),
    filled_slots     INTEGER      NOT NULL CHECK (filled_slots >= 0),
    candidate_count  INTEGER      NOT NULL CHECK (candidate_count >= 0),
    no_fill_reason   VARCHAR(50),
    created_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_ad_decisions_placement
        FOREIGN KEY (placement_id)
        REFERENCES placements(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_ad_decisions_filled_within_requested
        CHECK (filled_slots <= requested_slots)
);

CREATE INDEX idx_ad_decisions_request_id
    ON ad_decisions(request_id);

CREATE INDEX idx_ad_decisions_created_at
    ON ad_decisions(created_at);

-- One row per filled slot.
CREATE TABLE ad_decision_slots (
    id             BIGSERIAL        PRIMARY KEY,
    decision_id    UUID             NOT NULL,
    slot_position  INTEGER          NOT NULL CHECK (slot_position > 0),
    campaign_id    BIGINT           NOT NULL,
    creative_id    BIGINT           NOT NULL,
    -- Price at auction time. A click is charged this amount,
    -- even if the advertiser changes the bid afterwards.
    price_cpc      NUMERIC(10,2)    NOT NULL CHECK (price_cpc > 0),
    predicted_ctr  DOUBLE PRECISION NOT NULL
                       CHECK (predicted_ctr >= 0 AND predicted_ctr <= 1),
    ranking_score  DOUBLE PRECISION NOT NULL,
    click_token    VARCHAR(64)      NOT NULL,
    created_at     TIMESTAMPTZ      NOT NULL,

    CONSTRAINT fk_ad_decision_slots_decision
        FOREIGN KEY (decision_id)
        REFERENCES ad_decisions(decision_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ad_decision_slots_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ad_decision_slots_creative
        FOREIGN KEY (creative_id)
        REFERENCES creatives(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_ad_decision_slots_position
        UNIQUE (decision_id, slot_position),

    -- A campaign can hold at most one slot per decision.
    CONSTRAINT uq_ad_decision_slots_campaign
        UNIQUE (decision_id, campaign_id),

    CONSTRAINT uq_ad_decision_slots_click_token
        UNIQUE (click_token)
);

CREATE INDEX idx_ad_decision_slots_campaign_id
    ON ad_decision_slots(campaign_id);