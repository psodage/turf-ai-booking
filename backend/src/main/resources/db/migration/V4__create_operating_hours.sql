-- ============================================================
-- V4: Create operating_hours table
-- One record per day of week per turf (7 records per turf).
-- day_of_week: 0=Monday, 1=Tuesday, ..., 6=Sunday
-- Business rules: Bookings cannot be created outside these hours.
-- ============================================================

CREATE TABLE operating_hours (
    id           UUID    NOT NULL DEFAULT ${uuid_gen_func},
    turf_id      UUID    NOT NULL,
    day_of_week  INT     NOT NULL,
    opening_time TIME,
    closing_time TIME,
    is_closed    BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_operating_hours PRIMARY KEY (id),
    CONSTRAINT fk_operating_hours_turf FOREIGN KEY (turf_id) REFERENCES turf(id),
    -- Exactly one record per turf per day
    CONSTRAINT uq_operating_hours_turf_day UNIQUE (turf_id, day_of_week),
    CONSTRAINT chk_operating_hours_day_of_week CHECK (day_of_week BETWEEN 0 AND 6),
    -- When not closed, both times must be present and opening must be before closing
    CONSTRAINT chk_operating_hours_times
        CHECK (is_closed = TRUE OR (opening_time IS NOT NULL AND closing_time IS NOT NULL AND opening_time < closing_time))
);

CREATE INDEX idx_operating_hours_turf ON operating_hours(turf_id);

COMMENT ON TABLE operating_hours IS 'Daily opening hours per turf. One row per day of week (0=Mon to 6=Sun).';
COMMENT ON COLUMN operating_hours.day_of_week IS '0=Monday, 1=Tuesday, 2=Wednesday, 3=Thursday, 4=Friday, 5=Saturday, 6=Sunday';
COMMENT ON COLUMN operating_hours.is_closed IS 'TRUE means no bookings accepted on this day.';
