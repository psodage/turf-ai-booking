-- ============================================================
-- V10: Create blocked_slot table
-- Allows turf owners/managers to block specific slots from being booked.
-- Reasons: MAINTENANCE, PRIVATE_EVENT, OWNER_USE
-- ============================================================

CREATE TABLE blocked_slot (
    id         UUID        NOT NULL DEFAULT ${uuid_gen_func},
    turf_id    UUID        NOT NULL,
    date       DATE        NOT NULL,
    start_time TIME        NOT NULL,
    end_time   TIME        NOT NULL,
    reason     VARCHAR(30) NOT NULL,
    created_by UUID        NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_blocked_slot PRIMARY KEY (id),
    CONSTRAINT fk_blocked_slot_turf FOREIGN KEY (turf_id) REFERENCES turf(id),
    CONSTRAINT fk_blocked_slot_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_blocked_slot_reason CHECK (reason IN ('MAINTENANCE', 'PRIVATE_EVENT', 'OWNER_USE')),
    CONSTRAINT chk_blocked_slot_times CHECK (start_time < end_time)
);

-- Fast lookup of blocked slots for availability checks
CREATE INDEX idx_blocked_slot_turf_date ON blocked_slot(turf_id, date);

COMMENT ON TABLE blocked_slot IS 'Periods blocked by turf owner/manager.';
COMMENT ON COLUMN blocked_slot.reason IS 'MAINTENANCE | PRIVATE_EVENT | OWNER_USE';
