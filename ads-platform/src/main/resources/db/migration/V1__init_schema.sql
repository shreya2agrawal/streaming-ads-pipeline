
-- CAMPAIGMS TABLE

CREATE TABLE campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    advertiser VARCHAR(255) NOT NULL,
    agency VARCHAR(255) NOT NULL,
    budget NUMERIC(15,2) NOT NULL,
    flight_start DATE NOT NULL,
    flight_end DATE NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','PAUSED','COMPLETED','AT_RISK','PACING_LOW')),
    pacing_percent DOUBLE PRECISION,
    impressions_delivered BIGINT DEFAULT 0,
    impressions_goal BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaigns_status ON campaigns(status);

CREATE INDEX idx_campaigns_advertiser ON campaigns(advertiser);

-- INVENTORY TABLE

CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    show_id VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    content_rating VARCHAR(20) NOT NULL,
    total_slots BIGINT NOT NULL DEFAULT 0,
    allocated_slots BIGINT NOT NULL DEFAULT 0,
    reserved_slots BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE' 
        CHECK (status IN ('AVAILABLE','NEAR_SOLD_OUT','SOLD_OUT')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_genre
    ON inventory(genre);

CREATE INDEX idx_inventory_status
    ON inventory(status);

-- MEDIA PLANS TABLE

CREATE TABLE media_plans (
    id                    BIGSERIAL        PRIMARY KEY,
    name                  VARCHAR(255)     NOT NULL,
    advertiser            VARCHAR(255)     NOT NULL,
    agency                VARCHAR(255)     NOT NULL,
    total_budget          NUMERIC(15,2)    NOT NULL,
    flight_start          DATE             NOT NULL,
    flight_end            DATE             NOT NULL,
    status                VARCHAR(30)      NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT','PROPOSAL_SENT','NEGOTIATING',
                          'APPROVED','ACTIVE','COMPLETED','CANCELLED'
                          )),
    plan_type             VARCHAR(30)      NOT NULL
                          CHECK (plan_type IN (
                          'UPFRONT','PROGRAMMATIC','DIRECT','RFP'
                          )),
    total_slots_requested BIGINT           NOT NULL,
    total_slots_allocated BIGINT           NOT NULL DEFAULT 0,
    completion_rate       DOUBLE PRECISION          DEFAULT 0.0,
    rfp_reference         VARCHAR(100),
    notes                 TEXT,
    activated_at          TIMESTAMP,              
    created_at            TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP        NOT NULL DEFAULT NOW()    
);

CREATE INDEX idx_media_plans_status
    ON media_plans(status);

CREATE INDEX idx_media_plans_advertiser
    ON media_plans(advertiser);

CREATE TABLE media_plan_show_ids (
    media_plan_id         BIGINT           NOT NULL
                              REFERENCES media_plans(id)
                              ON DELETE CASCADE,
    show_id               VARCHAR(100)     NOT NULL,
    PRIMARY KEY (media_plan_id, show_id)
);

CREATE TABLE media_plan_targeting (
    media_plan_id         BIGINT           NOT NULL
                              REFERENCES media_plans(id)
                              ON DELETE CASCADE,
    segment               VARCHAR(100)     NOT NULL,
    PRIMARY KEY (media_plan_id, segment)
);

-- SAFETY FLAGS TABLE
CREATE TABLE safety_flags (
    id                    BIGSERIAL        PRIMARY KEY,\
    creative_file         VARCHAR(500)     NOT NULL,
    advertiser            VARCHAR(255)     NOT NULL,
    reason                TEXT             NOT NULL,
    severity              VARCHAR(30)      NOT NULL
                              CHECK (severity IN (
                                  'ADVISORY','TIER_3','POLICY_VIOLATION'
                              )),
    flag_status           VARCHAR(20)      NOT NULL DEFAULT 'PENDING'
                              CHECK (flag_status IN (
                                  'PENDING','APPROVED','REJECTED'
                              )),
    reviewed_by           VARCHAR(255),
    created_at            TIMESTAMP        NOT NULL DEFAULT NOW(),
    reviewed_at           TIMESTAMP
);

CREATE INDEX idx_safety_flags_status
    ON safety_flags(flag_status);

CREATE INDEX idx_safety_flags_severity
    ON safety_flags(severity);

-- CAMPAIGN METRICS AGGREGATED TABLE
CREATE TABLE campaign_metrics_aggregated (

    id                    BIGSERIAL        PRIMARY KEY,
    campaign_id           BIGINT
                              REFERENCES campaigns(id)
                              ON DELETE SET NULL,
    show_id               VARCHAR(100)     NOT NULL,
    window_start          TIMESTAMP        NOT NULL,
    window_end            TIMESTAMP        NOT NULL,
    impressions           BIGINT           NOT NULL DEFAULT 0,
    clicks                BIGINT           NOT NULL DEFAULT 0,
    ctr                   DOUBLE PRECISION          DEFAULT 0.0,
    bid_signal            VARCHAR(20)
                              CHECK (bid_signal IN (
                                  'BID_HIGH','BID_NORMAL','BID_LOW'
                              )),
    created_at            TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_metrics_window_start
    ON campaign_metrics_aggregated(window_start);

CREATE INDEX idx_metrics_campaign_id
    ON campaign_metrics_aggregated(campaign_id);

CREATE INDEX idx_metrics_show_id
    ON campaign_metrics_aggregated(show_id);
