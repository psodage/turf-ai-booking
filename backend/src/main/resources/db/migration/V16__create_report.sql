-- ============================================================
-- V16: Create report table
-- Stores metadata for generated Excel reports (ADR-012).
-- Files are stored temporarily on local filesystem and delivered via WhatsApp.
-- ============================================================

CREATE TABLE report (
    id           UUID        NOT NULL DEFAULT random_uuid(),
    business_id  UUID        NOT NULL,
    report_type  VARCHAR(20) NOT NULL,
    file_path    TEXT        NOT NULL,
    generated_by UUID,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_report PRIMARY KEY (id),
    CONSTRAINT fk_report_business     FOREIGN KEY (business_id)  REFERENCES business(id),
    CONSTRAINT fk_report_generated_by FOREIGN KEY (generated_by) REFERENCES users(id),
    CONSTRAINT chk_report_type CHECK (report_type IN ('DAILY', 'WEEKLY', 'MONTHLY'))
);

CREATE INDEX idx_report_business ON report(business_id);

COMMENT ON TABLE report IS 'Generated Excel report metadata (ADR-012). Files stored on local disk.';
