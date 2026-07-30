-- ============================================================
-- V22: Add preferred_language to conversation table
-- Persists user language context (EN, HI, MR, HINGLISH, MINGLISH) across turns.
-- ============================================================

ALTER TABLE conversation ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(20) DEFAULT 'EN';
COMMENT ON COLUMN conversation.preferred_language IS 'User preferred language context (EN, HI, MR, HINGLISH, MINGLISH).';
