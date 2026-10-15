CREATE TABLE campaign_target_shows (
    campaign_id BIGINT NOT NULL,
    show_id VARCHAR(255) NOT NULL,

    PRIMARY KEY (campaign_id, show_id),

    CONSTRAINT fk_campaign_target_shows_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE CASCADE
);

CREATE TABLE campaign_targeting_segments (
    campaign_id BIGINT NOT NULL,
    segment VARCHAR(255) NOT NULL,

    PRIMARY KEY (campaign_id, segment),

    CONSTRAINT fk_campaign_targeting_segments_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE CASCADE
);