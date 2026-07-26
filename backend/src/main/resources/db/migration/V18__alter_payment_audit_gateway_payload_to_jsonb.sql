-- ============================================================
-- V18: Alter payment_audit.gateway_payload from JSON to JSONB
-- Fixes Hibernate schema-validation mismatch: entity expects JSONB
-- but V15 originally created the column as JSON.
-- ============================================================

ALTER TABLE payment_audit
    ALTER COLUMN gateway_payload TYPE JSONB USING gateway_payload::JSONB;
