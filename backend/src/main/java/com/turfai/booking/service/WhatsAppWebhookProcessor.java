package com.turfai.booking.service;

import com.turfai.booking.dto.whatsapp.inbound.InboundWebhookPayload;
import com.turfai.booking.dto.whatsapp.inbound.WebhookChange;
import com.turfai.booking.dto.whatsapp.inbound.WebhookContact;
import com.turfai.booking.dto.whatsapp.inbound.WebhookEntry;
import com.turfai.booking.dto.whatsapp.inbound.WebhookInteractive;
import com.turfai.booking.dto.whatsapp.inbound.WebhookMessage;
import com.turfai.booking.dto.whatsapp.inbound.WebhookStatusUpdate;
import com.turfai.booking.dto.whatsapp.inbound.WebhookValue;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.MessageType;
import com.turfai.booking.entity.User;
import com.turfai.booking.exception.WebhookReplayException;
import com.turfai.booking.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppWebhookProcessor {

    public static final long MAX_REPLAY_AGE_SECONDS = 300; // 5 minutes (ADR-015)

    private final BusinessRepository businessRepository;
    private final CustomerRegistrationService customerRegistrationService;
    private final ConversationService conversationService;

    @Transactional
    public void processWebhookPayload(InboundWebhookPayload payload) {
        if (payload == null || payload.getEntry() == null) {
            return;
        }

        for (WebhookEntry entry : payload.getEntry()) {
            if (entry.getChanges() == null) continue;

            for (WebhookChange change : entry.getChanges()) {
                WebhookValue value = change.getValue();
                if (value == null) continue;

                // Handle Status Updates (delivery receipts)
                if (value.getStatuses() != null && !value.getStatuses().isEmpty()) {
                    for (WebhookStatusUpdate statusUpdate : value.getStatuses()) {
                        log.info("WhatsApp status update: messageId={}, status={}, recipient={}",
                                statusUpdate.getId(), statusUpdate.getStatus(), statusUpdate.getRecipientId());
                    }
                }

                // Handle Incoming Messages
                if (value.getMessages() != null && !value.getMessages().isEmpty()) {
                    String phoneNumberId = value.getMetadata() != null ? value.getMetadata().getPhoneNumberId() : null;
                    if (phoneNumberId == null) {
                        log.warn("Incoming message payload missing metadata.phone_number_id");
                        continue;
                    }

                    // ADR-006: Business Routing by connected WhatsApp Business phone_number_id
                    Optional<Business> businessOpt = businessRepository.findByWhatsappPhoneNumberId(phoneNumberId);
                    if (businessOpt.isEmpty()) {
                        log.warn("No Business found registered for WhatsApp phone_number_id: {}", phoneNumberId);
                        continue;
                    }
                    Business business = businessOpt.get();

                    for (WebhookMessage message : value.getMessages()) {
                        processIncomingMessage(message, value.getContacts(), business);
                    }
                }
            }
        }
    }

    private void processIncomingMessage(WebhookMessage message, java.util.List<WebhookContact> contacts, Business business) {
        // 1. Replay Protection (ADR-015): Verify timestamp is within last 5 minutes
        if (message.getTimestamp() != null) {
            long timestampEpoch = Long.parseLong(message.getTimestamp());
            long ageSeconds = Instant.now().getEpochSecond() - timestampEpoch;
            if (ageSeconds > MAX_REPLAY_AGE_SECONDS) {
                log.warn("Rejected replayed WhatsApp message {} (Age: {}s)", message.getId(), ageSeconds);
                throw new WebhookReplayException("Webhook message is older than 5 minutes threshold.");
            }
        }

        // 2. Message Deduplication (ADR-015): Check wamid
        String wamid = message.getId();
        if (conversationService.isMessageDuplicate(wamid)) {
            log.info("Ignoring duplicate WhatsApp message {}", wamid);
            return;
        }

        // 3. User Identification & Customer Auto-Registration (ADR-002)
        String senderPhone = message.getFrom();
        String senderName = extractSenderName(contacts, senderPhone);
        User user = customerRegistrationService.getOrCreateCustomer(senderPhone, senderName);

        // 4. Conversation Session & Lock (ADR-018)
        Conversation conversation = conversationService.getOrCreateActiveConversation(user, business);

        // 5. Parse Message Content and Type
        String textContent = "";
        MessageType messageType = MessageType.TEXT;

        if ("text".equalsIgnoreCase(message.getType()) && message.getText() != null) {
            textContent = message.getText().getBody();
            messageType = MessageType.TEXT;
        } else if ("interactive".equalsIgnoreCase(message.getType()) && message.getInteractive() != null) {
            WebhookInteractive interactive = message.getInteractive();
            if ("button_reply".equalsIgnoreCase(interactive.getType()) && interactive.getButtonReply() != null) {
                textContent = interactive.getButtonReply().getTitle();
                messageType = MessageType.BUTTON;
            } else if ("list_reply".equalsIgnoreCase(interactive.getType()) && interactive.getListReply() != null) {
                textContent = interactive.getListReply().getTitle();
                messageType = MessageType.LIST;
            }
        } else {
            textContent = "[Unsupported message type: " + message.getType() + "]";
            messageType = MessageType.TEXT;
        }

        // 6. Save Message into Conversation
        conversationService.saveIncomingMessage(conversation, textContent, messageType, wamid);
        log.info("Persisted incoming WhatsApp message from user {} at business {}: [{}]", user.getPhone(), business.getName(), textContent);
    }

    private String extractSenderName(java.util.List<WebhookContact> contacts, String phone) {
        if (contacts == null || contacts.isEmpty()) return null;
        return contacts.stream()
                .filter(c -> phone.equals(c.getWaId()))
                .map(c -> c.getProfile() != null ? c.getProfile().getName() : null)
                .findFirst()
                .orElse(null);
    }
}
