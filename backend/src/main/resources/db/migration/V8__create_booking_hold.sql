-- ============================================================
-- V8: Create booking_hold table
-- ADR-014: Simplified hold — slot data NOT duplicated here, lives on booking.
-- ADR-005: expires_at = NOW() + 10 minutes. Lazy expiry + cleanup job.
-- ADR-016: Grace period — check expires_at when payment webhook arrives late.
-- ============================================================

CREATE TABLE booking_hold (
    id         UUID        NOT NULL DEFAULT random_uuid(),
    booking_id UUID        NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_booking_hold PRIMARY KEY (id),
    CONSTRAINT fk_booking_hold_booking FOREIGN KEY (booking_id) REFERENCES booking(id),
    -- ADR-014: One hold per booking
    CONSTRAINT uq_booking_hold_booking UNIQUE (booking_id),
    CONSTRAINT chk_booking_hold_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CONVERTED'))
);

-- ============================================================
-- ADR-005: Cleanup job queries: WHERE status = 'ACTIVE' AND expires_at < NOW()
-- Status first (equality filter), then expires_at (range filter).
-- ============================================================
CREATE INDEX idx_booking_hold_status_expires ON booking_hold(status, expires_at);

COMMENT ON TABLE booking_hold IS 'Temporary slot reservation. Slot data lives on booking table (ADR-014).';
COMMENT ON COLUMN booking_hold.expires_at IS 'UTC expiry = creation time + 10 minutes (ADR-005).';
COMMENT ON COLUMN booking_hold.status IS 'ACTIVE | EXPIRED (cleanup job) | CONVERTED (payment confirmed).';
COMMENT ON INDEX idx_booking_hold_status_expires IS 'Optimized for 2-minute cleanup job (ADR-005): WHERE status=ACTIVE AND expires_at < NOW().';
