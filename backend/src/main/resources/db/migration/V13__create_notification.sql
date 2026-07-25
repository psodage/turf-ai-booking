-- ============================================================
-- V13: Create notification table
-- Stores outbound notifications (confirmations, reminders, cancellations).
-- ADR-017: Proactive outbound notifications use WhatsApp templates.
-- ============================================================

CREATE TABLE notification (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    booking_id  UUID,
    business_id UUID        NOT NULL,
    type        VARCHAR(30) NOT NULL,
    channel     VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP',
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT         NOT NULL DEFAULT 0,
    sent_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT fk_notification_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_notification_booking  FOREIGN KEY (booking_id)  REFERENCES booking(id),
    CONSTRAINT fk_notification_business FOREIGN KEY (business_id) REFERENCES business(id),
    CONSTRAINT chk_notification_type CHECK (type IN ('BOOKING_CONFIRMED', 'BOOKING_REMINDER', 'CANCELLATION', 'OWNER_NOTIFICATION')),
    CONSTRAINT chk_notification_channel CHECK (channel IN ('WHATSAPP')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_notification_user ON notification(user_id);
CREATE INDEX idx_notification_status ON notification(status) WHERE status = 'PENDING';

COMMENT ON TABLE notification IS 'Outbound notification records (WhatsApp templates — ADR-017).';
