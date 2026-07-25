-- ============================================================
-- V5: Create pricing_rule table
-- Resolution order: PEAK → WEEKEND → BASE (most specific wins).
-- Price is locked at booking hold creation time.
-- ============================================================

CREATE TABLE pricing_rule (
    id           UUID           NOT NULL DEFAULT random_uuid(),
    turf_id      UUID           NOT NULL,
    pricing_type VARCHAR(20)    NOT NULL,
    day_of_week  INT,
    start_time   TIME,
    end_time     TIME,
    amount       NUMERIC(10, 2) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_pricing_rule PRIMARY KEY (id),
    CONSTRAINT fk_pricing_rule_turf FOREIGN KEY (turf_id) REFERENCES turf(id),
    CONSTRAINT chk_pricing_type CHECK (pricing_type IN ('BASE', 'WEEKEND', 'PEAK')),
    CONSTRAINT chk_pricing_amount CHECK (amount >= 0),
    CONSTRAINT chk_pricing_day_of_week CHECK (day_of_week IS NULL OR day_of_week BETWEEN 0 AND 6),
    CONSTRAINT chk_pricing_peak_times
        CHECK (pricing_type != 'PEAK' OR (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time))
);

CREATE INDEX idx_pricing_rule_turf ON pricing_rule(turf_id);

COMMENT ON TABLE pricing_rule IS 'Slot pricing per turf. Resolution: PEAK > WEEKEND > BASE.';
COMMENT ON COLUMN pricing_rule.pricing_type IS 'BASE | WEEKEND | PEAK';
COMMENT ON COLUMN pricing_rule.day_of_week IS 'Used by PEAK rules to target specific days (0=Mon to 6=Sun).';
COMMENT ON COLUMN pricing_rule.amount IS 'Price in INR (paise stored as decimal).';
