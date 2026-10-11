CREATE TABLE placement_supported_devices (
    placement_id BIGINT NOT NULL,
    device_type VARCHAR(50) NOT NULL,

    PRIMARY KEY (placement_id, device_type),

    CONSTRAINT fk_placement_supported_devices
        FOREIGN KEY (placement_id)
        REFERENCES placements(id)
        ON DELETE CASCADE
);

CREATE TABLE placement_supported_creative_types (
    placement_id BIGINT NOT NULL,
    creative_type VARCHAR(50) NOT NULL,

    PRIMARY KEY (placement_id, creative_type),

    CONSTRAINT fk_placement_supported_creative_types
        FOREIGN KEY (placement_id)
        REFERENCES placements(id)
        ON DELETE CASCADE
);