-- ============================================================
-- V3: Create turf table
-- Each turf belongs to one business and has its own operating
-- hours and pricing rules.
-- ============================================================

CREATE TABLE turf (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    business_id UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    capacity    INT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_turf PRIMARY KEY (id),
    CONSTRAINT fk_turf_business FOREIGN KEY (business_id) REFERENCES business(id),
    CONSTRAINT chk_turf_type CHECK (type IN ('FIVE_A_SIDE', 'SEVEN_A_SIDE', 'ELEVEN_A_SIDE')),
    CONSTRAINT chk_turf_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_turf_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE INDEX idx_turf_business ON turf(business_id);

COMMENT ON TABLE turf IS 'Individual playable turfs owned by a business.';
COMMENT ON COLUMN turf.type IS 'FIVE_A_SIDE | SEVEN_A_SIDE | ELEVEN_A_SIDE';
COMMENT ON COLUMN turf.capacity IS 'Maximum players. NULL means unconfigured.';
