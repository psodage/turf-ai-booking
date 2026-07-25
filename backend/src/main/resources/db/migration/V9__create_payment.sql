-- ============================================================
-- V9: Create payment table
-- ADR-003: Booking → Payment is 1:N (multiple payment attempts).
-- Only a payment with status = SUCCESS confirms the booking.
-- ============================================================

CREATE TABLE payment (
    id                 UUID           NOT NULL DEFAULT random_uuid(),
    booking_id         UUID           NOT NULL,
    business_id        UUID           NOT NULL,
    customer_id        UUID           NOT NULL,
    gateway            VARCHAR(30)    NOT NULL DEFAULT 'RAZORPAY',
    gateway_order_id   VARCHAR(255),
    gateway_payment_id VARCHAR(255),
    amount             NUMERIC(10, 2) NOT NULL,
    currency           VARCHAR(10)    NOT NULL DEFAULT 'INR',
    status             VARCHAR(20)    NOT NULL DEFAULT 'CREATED',
    refund_status      VARCHAR(20)    NOT NULL DEFAULT 'NOT_REQUIRED',
    created_at         TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT fk_payment_booking  FOREIGN KEY (booking_id)  REFERENCES booking(id),
    CONSTRAINT fk_payment_business FOREIGN KEY (business_id) REFERENCES business(id),
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT chk_payment_gateway CHECK (gateway IN ('RAZORPAY')),
    CONSTRAINT chk_payment_status  CHECK (status IN ('CREATED', 'PENDING', 'SUCCESS', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_payment_refund  CHECK (refund_status IN ('NOT_REQUIRED', 'REQUESTED', 'PROCESSING', 'SUCCESS', 'FAILED')),
    CONSTRAINT chk_payment_amount  CHECK (amount >= 0)
);

-- ADR-003: Lookup all payments for a booking (1:N)
CREATE INDEX idx_payment_booking ON payment(booking_id);

-- Deduplication: Razorpay sends the same payment_id in retried webhooks
CREATE UNIQUE INDEX idx_payment_gateway_payment_id ON payment(gateway_payment_id);

-- Dashboard/reporting queries
CREATE INDEX idx_payment_status ON payment(status);

COMMENT ON TABLE payment IS 'Payment attempts per booking. One booking can have many payments (ADR-003).';
COMMENT ON COLUMN payment.gateway_payment_id IS 'Razorpay payment ID. Unique index for webhook deduplication.';
COMMENT ON COLUMN payment.refund_status IS 'NOT_REQUIRED | REQUESTED | PROCESSING | SUCCESS | FAILED';
