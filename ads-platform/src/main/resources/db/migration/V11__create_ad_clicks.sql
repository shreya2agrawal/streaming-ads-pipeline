-- One row per counted click. The unique token is what makes a
-- retried or double click count once: only one insert can win.
CREATE TABLE ad_clicks (
    id              BIGSERIAL     PRIMARY KEY,
    click_token     VARCHAR(64)   NOT NULL,
    decision_id     UUID          NOT NULL,
    campaign_id     BIGINT        NOT NULL,
    creative_id     BIGINT        NOT NULL,
    -- What was actually billed. Can be less than the slot price
    -- near the budget limit, or 0 once the budget is spent.
    charged_amount  NUMERIC(10,2) NOT NULL DEFAULT 0
                        CHECK (charged_amount >= 0),
    clicked_at      TIMESTAMPTZ   NOT NULL,

    CONSTRAINT uq_ad_clicks_click_token
        UNIQUE (click_token),

    -- A click can only exist for a token the platform issued.
    CONSTRAINT fk_ad_clicks_slot
        FOREIGN KEY (click_token)
        REFERENCES ad_decision_slots(click_token)
        ON DELETE RESTRICT,

    -- Billing records keep their decision: a decision with clicks
    -- cannot be deleted.
    CONSTRAINT fk_ad_clicks_decision
        FOREIGN KEY (decision_id)
        REFERENCES ad_decisions(decision_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ad_clicks_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_ad_clicks_campaign_id
    ON ad_clicks(campaign_id);

CREATE INDEX idx_ad_clicks_clicked_at
    ON ad_clicks(clicked_at);