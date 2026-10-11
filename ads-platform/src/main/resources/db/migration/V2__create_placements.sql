CREATE TABLE placements (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL,
    max_ads INTEGER,
    duration_seconds INTEGER
);