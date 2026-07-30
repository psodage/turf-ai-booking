-- ============================================================
-- V21: Add latitude and longitude columns to business table
-- Enables sending native WhatsApp location messages for turfs
-- ============================================================

ALTER TABLE business ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE business ADD COLUMN longitude DOUBLE PRECISION;

COMMENT ON COLUMN business.latitude IS 'Geographic latitude coordinate of the business.';
COMMENT ON COLUMN business.longitude IS 'Geographic longitude coordinate of the business.';
