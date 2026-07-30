package com.turfai.booking.ai;

import com.turfai.booking.ai.config.AiProperties;
import com.turfai.booking.ai.memory.ConversationContextBuilder;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationMessage;
import com.turfai.booking.entity.ConversationStatus;
import com.turfai.booking.entity.MessageSender;
import com.turfai.booking.repository.ConversationMessageRepository;
import com.turfai.booking.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationContextBuilderTest {

    @Mock private ConversationMessageRepository conversationMessageRepository;
    @Mock private ConversationRepository conversationRepository;

    private AiProperties aiProperties;
    private ConversationContextBuilder conversationContextBuilder;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setMaxContextMessages(10);
        aiProperties.setSessionTimeoutMinutes(10);

        conversationContextBuilder = new ConversationContextBuilder(aiProperties, conversationMessageRepository, conversationRepository);
    }

    @Test
    @DisplayName("Should truncate message history to last 10 messages (sliding window)")
    void testSlidingWindowTruncation() {
        Conversation conv = Conversation.builder().id(UUID.randomUUID()).status(ConversationStatus.ACTIVE).lastActivity(Instant.now()).build();

        List<ConversationMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            messages.add(ConversationMessage.builder()
                    .conversation(conv)
                    .sender(i % 2 == 0 ? MessageSender.AI : MessageSender.USER)
                    .message("Message " + i)
                    .build());
        }

        when(conversationMessageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId())).thenReturn(messages);

        List<Map<String, String>> history = conversationContextBuilder.buildMessageHistory(conv);

        assertThat(history).hasSize(10);
        assertThat(history.get(0).get("content")).isEqualTo("Message 6");
        assertThat(history.get(9).get("content")).isEqualTo("Message 15");
    }

    @Test
    @DisplayName("Should mark conversation expired if inactive for more than 10 minutes")
    void testSessionTimeoutExpiry() {
        Instant oldActivity = Instant.now().minus(15, ChronoUnit.MINUTES);
        Conversation conv = Conversation.builder().id(UUID.randomUUID()).status(ConversationStatus.ACTIVE).lastActivity(oldActivity).build();

        List<Map<String, String>> history = conversationContextBuilder.buildMessageHistory(conv);

        assertThat(history).isEmpty();
        assertThat(conv.getStatus()).isEqualTo(ConversationStatus.EXPIRED);
        verify(conversationRepository).save(conv);
    }
}
