CREATE TABLE creatives (
    id BIGSERIAL PRIMARY KEY,

    campaign_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,

    creative_type VARCHAR(50) NOT NULL,

    media_url TEXT NOT NULL,
    destination_url TEXT NOT NULL,
    content_text TEXT NOT NULL,

    duration_seconds INTEGER,

    status VARCHAR(50) NOT NULL,
    safety_status VARCHAR(50) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_creatives_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_creatives_campaign_id
    ON creatives(campaign_id);