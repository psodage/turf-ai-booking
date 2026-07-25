package com.turfai.booking.service;

import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationMessage;
import com.turfai.booking.entity.ConversationStatus;
import com.turfai.booking.entity.MessageSender;
import com.turfai.booking.entity.MessageType;
import com.turfai.booking.entity.User;
import com.turfai.booking.repository.ConversationMessageRepository;
import com.turfai.booking.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    /**
     * Finds active conversation with pessimistic lock (ADR-018) or creates a new one.
     */
    @Transactional
    public Conversation getOrCreateActiveConversation(User user, Business business) {
        Optional<Conversation> convOpt = conversationRepository.findWithLockByUserIdAndBusinessIdAndStatus(
                user.getId(), business.getId(), ConversationStatus.ACTIVE);

        if (convOpt.isPresent()) {
            Conversation conv = convOpt.get();
            conv.setLastActivity(Instant.now());
            return conversationRepository.save(conv);
        }

        log.info("Creating new WhatsApp conversation session for user {} at business {}", user.getPhone(), business.getName());
        Conversation newConv = Conversation.builder()
                .user(user)
                .business(business)
                .role(user.getRole())
                .status(ConversationStatus.ACTIVE)
                .lastActivity(Instant.now())
                .build();

        return conversationRepository.save(newConv);
    }

    @Transactional
    public ConversationMessage saveIncomingMessage(Conversation conversation, String messageText, MessageType messageType, String whatsappMessageId) {
        conversation.setLastActivity(Instant.now());
        conversationRepository.save(conversation);

        ConversationMessage msg = ConversationMessage.builder()
                .conversation(conversation)
                .sender(MessageSender.USER)
                .message(messageText)
                .messageType(messageType != null ? messageType : MessageType.TEXT)
                .whatsappMessageId(whatsappMessageId)
                .build();

        return conversationMessageRepository.save(msg);
    }

    @Transactional
    public ConversationMessage saveOutgoingMessage(Conversation conversation, String messageText, MessageType messageType) {
        conversation.setLastActivity(Instant.now());
        conversationRepository.save(conversation);

        ConversationMessage msg = ConversationMessage.builder()
                .conversation(conversation)
                .sender(MessageSender.AI)
                .message(messageText)
                .messageType(messageType != null ? messageType : MessageType.TEXT)
                .build();

        return conversationMessageRepository.save(msg);
    }

    @Transactional(readOnly = true)
    public boolean isMessageDuplicate(String whatsappMessageId) {
        if (whatsappMessageId == null || whatsappMessageId.isBlank()) {
            return false;
        }
        return conversationMessageRepository.findByWhatsappMessageId(whatsappMessageId).isPresent();
    }
}
