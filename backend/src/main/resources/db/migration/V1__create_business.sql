-- ============================================================
-- V1: Create business table
-- ADR-006: whatsapp_phone_number_id for webhook routing
-- ADR-019: timezone stored per business for local-time rule evaluation
-- ============================================================

CREATE TABLE business (
    id                         UUID        NOT NULL DEFAULT gen_random_uuid(),
    name                       VARCHAR(255) NOT NULL,
    address                    TEXT,
    city                       VARCHAR(100),
    state                      VARCHAR(100),
    pincode                    VARCHAR(20),
    google_maps_link           TEXT,
    phone                      VARCHAR(20),
    whatsapp_phone_number_id   VARCHAR(100) NOT NULL,
    timezone                   VARCHAR(100) NOT NULL DEFAULT 'Asia/Kolkata',
    status                     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_business PRIMARY KEY (id),
    CONSTRAINT uq_business_whatsapp_phone_number_id UNIQUE (whatsapp_phone_number_id),
    CONSTRAINT chk_business_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

-- ADR-006: Fast lookup of business by WhatsApp phone number on every inbound webhook
CREATE UNIQUE INDEX idx_business_whatsapp_phone_number_id ON business(whatsapp_phone_number_id);

COMMENT ON TABLE business IS 'Turf business tenants. Each business owns one WhatsApp Business number.';
COMMENT ON COLUMN business.whatsapp_phone_number_id IS 'Maps this business to its WhatsApp Business phone number for webhook routing (ADR-006).';
COMMENT ON COLUMN business.timezone IS 'IANA timezone string e.g. Asia/Kolkata (ADR-019). Business rules evaluated in this timezone.';
COMMENT ON COLUMN business.status IS 'ACTIVE | INACTIVE | SUSPENDED';
