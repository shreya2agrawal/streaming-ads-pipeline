-- One row per booking an advertiser reports for a click.
-- The booking happens on the advertiser's site; their server reports it
-- with the click token, so it can be credited to the exact click.
CREATE TABLE ad_conversions (
    id            BIGSERIAL      PRIMARY KEY,
    click_token   VARCHAR(64)    NOT NULL,
    campaign_id   BIGINT         NOT NULL,
    booking_id    VARCHAR(100)   NOT NULL,
    value         NUMERIC(12, 2) NOT NULL,
    currency      CHAR(3)        NOT NULL,
    status        VARCHAR(20)    NOT NULL,
    converted_at  TIMESTAMPTZ    NOT NULL,   -- when the booking happened (advertiser's time)
    received_at   TIMESTAMPTZ    NOT NULL,   -- when the platform was told
    cancelled_at  TIMESTAMPTZ,

    -- A booking is counted once per campaign, however often it is reported.
    CONSTRAINT uq_ad_conversions_campaign_booking
        UNIQUE (campaign_id, booking_id),

    -- A conversion must belong to a recorded click.
    CONSTRAINT fk_ad_conversions_click
        FOREIGN KEY (click_token)
        REFERENCES ad_clicks(click_token)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ad_conversions_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_ad_conversions_value
        CHECK (value >= 0),

    CONSTRAINT ck_ad_conversions_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED')),

    -- cancelled_at is set exactly when the conversion is cancelled.
    CONSTRAINT ck_ad_conversions_cancelled_at
        CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL))
);

-- Advertiser stats read conversions per campaign over a date range.
CREATE INDEX idx_ad_conversions_campaign_time
    ON ad_conversions(campaign_id, converted_at);