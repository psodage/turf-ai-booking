-- ============================================================
-- V15: Create payment_audit table
-- Audit history of payment gateway events and raw webhook payloads (JSONB).
-- ============================================================

CREATE TABLE payment_audit (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    payment_id      UUID        NOT NULL,
    event           VARCHAR(100) NOT NULL,
    gateway_payload JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_payment_audit PRIMARY KEY (id),
    CONSTRAINT fk_payment_audit_payment FOREIGN KEY (payment_id) REFERENCES payment(id)
);

CREATE INDEX idx_payment_audit_payment ON payment_audit(payment_id);

COMMENT ON TABLE payment_audit IS 'Audit trail recording raw gateway payloads and payment events.';
