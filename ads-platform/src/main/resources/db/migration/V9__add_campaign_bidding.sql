-- Bidding and spend on campaigns.
--
-- bid_cpc is nullable on purpose: existing campaigns have no bid,
-- and a default would silently enter them into auctions at a price
-- nobody chose. Candidate retrieval skips campaigns without a bid.
ALTER TABLE campaigns
    ADD COLUMN bid_cpc NUMERIC(10,2),
    ADD COLUMN spent   NUMERIC(15,2) NOT NULL DEFAULT 0;

ALTER TABLE campaigns
    ADD CONSTRAINT chk_campaigns_bid_cpc_positive
        CHECK (bid_cpc IS NULL OR bid_cpc > 0),
    ADD CONSTRAINT chk_campaigns_spent_non_negative
        CHECK (spent >= 0),
    -- Last line of defense against overspend, even if application
    -- code has a bug. A budget can also never be lowered below
    -- what has already been spent.
    ADD CONSTRAINT chk_campaigns_spent_within_budget
        CHECK (spent <= budget);