-- ============================================================
-- V14: Create booking_audit table
-- Audit history of all status transitions for bookings.
-- ============================================================

CREATE TABLE booking_audit (
    id         UUID        NOT NULL DEFAULT random_uuid(),
    booking_id UUID        NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by UUID,
    reason     TEXT,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_booking_audit PRIMARY KEY (id),
    CONSTRAINT fk_booking_audit_booking    FOREIGN KEY (booking_id) REFERENCES booking(id),
    CONSTRAINT fk_booking_audit_changed_by FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX idx_booking_audit_booking ON booking_audit(booking_id);

COMMENT ON TABLE booking_audit IS 'Audit trail recording every booking status transition.';
