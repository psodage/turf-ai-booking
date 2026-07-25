package com.turfai.booking.ai.memory;

import com.turfai.booking.ai.config.AiProperties;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationMessage;
import com.turfai.booking.entity.ConversationStatus;
import com.turfai.booking.entity.MessageSender;
import com.turfai.booking.repository.ConversationMessageRepository;
import com.turfai.booking.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationContextBuilder {

    private final AiProperties aiProperties;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationRepository conversationRepository;

    @Transactional
    public List<Map<String, String>> buildMessageHistory(Conversation conversation) {
        // 1. Session Inactivity Timeout Check (30 minutes)
        if (conversation.getLastActivity() != null) {
            long inactiveMinutes = Duration.between(conversation.getLastActivity(), Instant.now()).toMinutes();
            if (inactiveMinutes >= aiProperties.getSessionTimeoutMinutes()) {
                log.info("Conversation session {} expired due to {} minutes of inactivity.", conversation.getId(), inactiveMinutes);
                conversation.setStatus(ConversationStatus.EXPIRED);
                conversationRepository.save(conversation);
                return List.of();
            }
        }

        // 2. Fetch all messages for conversation
        List<ConversationMessage> allMessages = conversationMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // 3. Sliding Window: Take last N messages (e.g. 10)
        int maxMessages = aiProperties.getMaxContextMessages();
        int startIndex = Math.max(0, allMessages.size() - maxMessages);
        List<ConversationMessage> windowedMessages = allMessages.subList(startIndex, allMessages.size());

        List<Map<String, String>> history = new ArrayList<>();
        for (ConversationMessage msg : windowedMessages) {
            Map<String, String> map = new HashMap<>();
            String role = msg.getSender() == MessageSender.USER ? "user" : "assistant";
            map.put("role", role);
            map.put("content", msg.getMessage() != null ? msg.getMessage() : "");
            history.add(map);
        }

        return history;
    }
}
