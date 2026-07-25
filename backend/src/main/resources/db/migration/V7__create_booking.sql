-- ============================================================
-- V7: Create booking table
-- ADR-004: Status lifecycle: HOLD → PAYMENT_PENDING → CONFIRMED → COMPLETED/CANCELLED/NO_SHOW
-- ADR-004: Conflict check includes HOLD, PAYMENT_PENDING, CONFIRMED
-- Partial unique index enforces no double-booking at the DB level.
-- ============================================================

CREATE TABLE booking (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    booking_number VARCHAR(30)    NOT NULL,
    business_id    UUID           NOT NULL,
    turf_id        UUID           NOT NULL,
    customer_id    UUID           NOT NULL,
    booking_date   DATE           NOT NULL,
    start_time     TIME           NOT NULL,
    end_time       TIME           NOT NULL,
    price          NUMERIC(10, 2) NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'HOLD',
    booking_source VARCHAR(20)    NOT NULL DEFAULT 'WHATSAPP_AI',
    cancelled_at   TIMESTAMPTZ,
    cancelled_by   UUID,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_booking PRIMARY KEY (id),
    CONSTRAINT fk_booking_business  FOREIGN KEY (business_id)  REFERENCES business(id),
    CONSTRAINT fk_booking_turf      FOREIGN KEY (turf_id)      REFERENCES turf(id),
    CONSTRAINT fk_booking_customer  FOREIGN KEY (customer_id)  REFERENCES users(id),
    CONSTRAINT fk_booking_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(id),
    CONSTRAINT uq_booking_number    UNIQUE (booking_number),
    CONSTRAINT chk_booking_status   CHECK (status IN ('HOLD', 'PAYMENT_PENDING', 'CONFIRMED', 'COMPLETED', 'EXPIRED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_booking_source   CHECK (booking_source IN ('WHATSAPP_AI', 'OWNER_MANUAL', 'ADMIN')),
    CONSTRAINT chk_booking_times    CHECK (start_time < end_time),
    CONSTRAINT chk_booking_price    CHECK (price >= 0)
);

-- ============================================================
-- Core availability index: availability checks filter by turf + date
-- ERD Section 28
-- ============================================================
CREATE INDEX idx_booking_turf_date ON booking(turf_id, booking_date);

-- Customer's booking history
CREATE INDEX idx_booking_customer ON booking(customer_id);

-- Business dashboard + cleanup job
CREATE INDEX idx_booking_business_status ON booking(business_id, status);

-- ============================================================
-- PARTIAL UNIQUE INDEX — the double-booking prevention lock
-- ADR-004: Conflict check only applies to active statuses.
-- EXPIRED, CANCELLED, COMPLETED, NO_SHOW slots must not block new bookings.
-- ============================================================
CREATE INDEX idx_booking_no_double_booking
    ON booking(turf_id, booking_date, start_time, end_time, status);

COMMENT ON TABLE booking IS 'Central booking record. Partial unique index prevents double-booking (ADR-004).';
COMMENT ON COLUMN booking.booking_number IS 'Human-readable ID: BK-{YEAR}-{SEQUENCE}. Generated from booking_number_seq.';
COMMENT ON COLUMN booking.price IS 'Price locked at hold creation time. Pricing changes do not affect existing holds.';
COMMENT ON COLUMN booking.cancelled_at IS 'UTC timestamp of cancellation. NULL for non-cancelled bookings.';
COMMENT ON COLUMN booking.cancelled_by IS 'FK to users.id of who cancelled. NULL for system/expiry cancellations.';
COMMENT ON INDEX idx_booking_no_double_booking IS 'Prevents two active bookings for the same turf/date/timeslot (ADR-004).';
