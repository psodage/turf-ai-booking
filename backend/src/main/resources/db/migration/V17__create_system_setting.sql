-- ============================================================
-- V17: Create system_setting table
-- Stores configurable key-value pairs for global system defaults.
-- ============================================================

CREATE TABLE system_setting (
    key         VARCHAR(100) NOT NULL,
    value       TEXT         NOT NULL,
    description TEXT,

    CONSTRAINT pk_system_setting PRIMARY KEY (key)
);

COMMENT ON TABLE system_setting IS 'System-wide configuration settings and defaults.';
