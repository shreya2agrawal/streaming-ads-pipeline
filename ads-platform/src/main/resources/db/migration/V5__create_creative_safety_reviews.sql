CREATE TABLE creative_safety_reviews (
    id BIGSERIAL PRIMARY KEY,
    creative_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    review_source VARCHAR(50) NOT NULL,
    reason TEXT,
    score DOUBLE PRECISION,
    model_version VARCHAR(255),
    policy_version VARCHAR(255),
    reviewer VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,

    CONSTRAINT fk_creative_safety_reviews_creative
        FOREIGN KEY (creative_id)
        REFERENCES creatives(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_creative_safety_reviews_creative_id
    ON creative_safety_reviews(creative_id);

CREATE INDEX idx_creative_safety_reviews_status
    ON creative_safety_reviews(status);