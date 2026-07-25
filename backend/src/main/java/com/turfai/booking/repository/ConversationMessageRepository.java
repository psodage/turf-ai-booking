package com.turfai.booking.repository;

import com.turfai.booking.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * ADR-015: Deduplication lookup by Meta WhatsApp Message ID (wamid).
     */
    Optional<ConversationMessage> findByWhatsappMessageId(String whatsappMessageId);
}
