-- ============================================================
-- V6: Create booking_number_seq sequence
-- Generates the numeric part of booking_number (BK-{YEAR}-{SEQ}).
-- ERD: Global sequence, collision-proof, no per-business reset.
-- Note: Annual reset is handled in application code by checking
-- the year portion; this sequence is monotonically increasing.
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS booking_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    NO CYCLE
    CACHE 1;

COMMENT ON SEQUENCE booking_number_seq IS 'Generates the numeric suffix for booking_number (BK-YYYY-NNNNN). Global sequence, never resets.';
