CREATE TABLE campaign_target_placements (
    campaign_id BIGINT NOT NULL,
    placement_id BIGINT NOT NULL,

    PRIMARY KEY (campaign_id, placement_id),

    CONSTRAINT fk_campaign_target_placements_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_campaign_target_placements_placement
        FOREIGN KEY (placement_id)
        REFERENCES placements(id)
        ON DELETE RESTRICT
);

CREATE TABLE campaign_target_devices (
    campaign_id BIGINT NOT NULL,
    device_type VARCHAR(50) NOT NULL,

    PRIMARY KEY (campaign_id, device_type),

    CONSTRAINT fk_campaign_target_devices_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns(id)
        ON DELETE CASCADE
);