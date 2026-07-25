-- ============================================================
-- V11: Create conversation table
-- Stores WhatsApp conversations between users (customers or owners)
-- and the AI assistant for a business.
-- ADR-018: Pessimistic locking (SELECT FOR UPDATE) on conversation row.
-- ============================================================

CREATE TABLE conversation (
    id             UUID        NOT NULL DEFAULT random_uuid(),
    user_id        UUID        NOT NULL,
    business_id    UUID        NOT NULL,
    role           VARCHAR(20) NOT NULL,
    current_intent VARCHAR(100),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_activity  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_conversation PRIMARY KEY (id),
    CONSTRAINT fk_conversation_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_conversation_business FOREIGN KEY (business_id) REFERENCES business(id),
    CONSTRAINT chk_conversation_role    CHECK (role IN ('CUSTOMER', 'OWNER', 'MANAGER', 'ADMIN')),
    CONSTRAINT chk_conversation_status  CHECK (status IN ('ACTIVE', 'CLOSED', 'EXPIRED'))
);

-- Fast lookup of active conversation for incoming WhatsApp message + ADR-018 lock
CREATE INDEX idx_conversation_user_business ON conversation(user_id, business_id);

COMMENT ON TABLE conversation IS 'WhatsApp conversation context. Rows locked via SELECT FOR UPDATE for concurrency (ADR-018).';
COMMENT ON COLUMN conversation.current_intent IS 'Current conversation intent tracked by AI agent.';
