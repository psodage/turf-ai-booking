-- ============================================================
-- V2: Create users table
-- ADR-002: Customers have NULL business_id (global users).
--          OWNER and MANAGER must have a business_id.
-- ============================================================

CREATE TABLE users (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    business_id UUID,
    name        VARCHAR(255) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    email       VARCHAR(255),
    role        VARCHAR(20)  NOT NULL,
    language    VARCHAR(10)  NOT NULL DEFAULT 'en',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT fk_users_business FOREIGN KEY (business_id) REFERENCES business(id),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'OWNER', 'MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    -- ADR-002: OWNER and MANAGER must be linked to a business
    CONSTRAINT chk_users_business_required_for_staff
        CHECK (role NOT IN ('OWNER', 'MANAGER') OR business_id IS NOT NULL)
);

-- Lookup user by WhatsApp phone number on every inbound message
CREATE UNIQUE INDEX idx_users_phone ON users(phone);

-- Filter business staff by role
CREATE INDEX idx_users_business_role ON users(business_id, role)
    WHERE business_id IS NOT NULL;

COMMENT ON TABLE users IS 'Customers, owners, managers, and admins. Customers have NULL business_id (ADR-002).';
COMMENT ON COLUMN users.business_id IS 'NULL for CUSTOMER and ADMIN roles. Required for OWNER and MANAGER (ADR-002).';
COMMENT ON COLUMN users.phone IS 'WhatsApp phone number. Unique across the system — one phone = one user.';
