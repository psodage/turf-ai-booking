-- ============================================================
-- V12: Create conversation_message table
-- Stores individual messages within a conversation.
-- Includes whatsapp_message_id for message deduplication (ADR-015).
-- ============================================================

CREATE TABLE conversation_message (
    id                  UUID        NOT NULL DEFAULT random_uuid(),
    conversation_id     UUID        NOT NULL,
    sender              VARCHAR(10) NOT NULL,
    message             TEXT        NOT NULL,
    message_type        VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    whatsapp_message_id VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_conversation_message PRIMARY KEY (id),
    CONSTRAINT fk_conversation_message_conv FOREIGN KEY (conversation_id) REFERENCES conversation(id),
    CONSTRAINT chk_conversation_message_sender CHECK (sender IN ('USER', 'AI')),
    CONSTRAINT chk_conversation_message_type CHECK (message_type IN ('TEXT', 'BUTTON', 'LIST', 'LOCATION'))
);

CREATE INDEX idx_conversation_message_conv ON conversation_message(conversation_id);

-- Deduplication index for WhatsApp webhook messages (ADR-015)
CREATE UNIQUE INDEX idx_conversation_message_wamid ON conversation_message(whatsapp_message_id);

COMMENT ON TABLE conversation_message IS 'Individual messages exchanged in a conversation.';
COMMENT ON COLUMN conversation_message.whatsapp_message_id IS 'Meta wamid for message deduplication (ADR-015).';
