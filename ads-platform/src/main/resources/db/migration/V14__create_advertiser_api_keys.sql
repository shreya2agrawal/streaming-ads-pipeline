-- API keys for advertisers' servers (server-to-server calls, such as
-- reporting conversions). Only a SHA-256 hash of each key is stored:
-- the key itself is shown once, when it is issued, and never again.
CREATE TABLE advertiser_api_keys (
    id            BIGSERIAL     PRIMARY KEY,
    advertiser    VARCHAR(255)  NOT NULL,   -- matches campaigns.advertiser
    key_prefix    VARCHAR(16)   NOT NULL,   -- first characters, safe to show in logs
    key_hash      CHAR(64)      NOT NULL,   -- SHA-256 of the full key, in hex
    created_at    TIMESTAMPTZ   NOT NULL,
    revoked_at    TIMESTAMPTZ,

    -- Each key is unique, and is looked up by its hash.
    CONSTRAINT uq_advertiser_api_keys_hash
        UNIQUE (key_hash),

    CONSTRAINT uq_advertiser_api_keys_prefix
        UNIQUE (key_prefix)
);

CREATE INDEX idx_advertiser_api_keys_advertiser
    ON advertiser_api_keys(advertiser);