-- The column holds the maximum length of each ad, not of the whole break.
ALTER TABLE placements
    RENAME COLUMN duration_seconds TO max_ad_duration_seconds;

-- max_ads becomes required. Rows without a value get 1,
-- the most conservative capacity.
UPDATE placements SET max_ads = 1 WHERE max_ads IS NULL;

ALTER TABLE placements
    ALTER COLUMN max_ads SET NOT NULL;

-- A duration limit only applies to placements that accept video.
-- Clear it everywhere else so existing rows satisfy the entity rule.
UPDATE placements p
SET max_ad_duration_seconds = NULL
WHERE p.max_ad_duration_seconds IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM placement_supported_creative_types t
      WHERE t.placement_id = p.id
        AND t.creative_type = 'VIDEO'
  );

ALTER TABLE placements
    ADD CONSTRAINT chk_placements_max_ads_positive
        CHECK (max_ads >= 1),
    ADD CONSTRAINT chk_placements_max_ad_duration_positive
        CHECK (max_ad_duration_seconds IS NULL OR max_ad_duration_seconds > 0);

-- Names are unique ignoring case, matching the service check.
-- This index is the real guarantee: two concurrent creates can both
-- pass the service check, but only one insert can succeed here.
CREATE UNIQUE INDEX uq_placements_name_lower
    ON placements (LOWER(name));