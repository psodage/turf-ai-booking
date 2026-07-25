package com.turfai.booking.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.ai.memory.ConversationContextBuilder;
import com.turfai.booking.ai.orchestrator.AiOrchestratorService;
import com.turfai.booking.ai.prompt.PromptManager;
import com.turfai.booking.ai.provider.MockAiProvider;
import com.turfai.booking.ai.tool.AiToolGateway;
import com.turfai.booking.ai.tool.ToolResult;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationStatus;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.service.ConversationService;
import com.turfai.booking.service.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiOrchestratorServiceTest {

    @Mock private PromptManager promptManager;
    @Mock private ConversationContextBuilder conversationContextBuilder;
    @Mock private AiToolGateway aiToolGateway;
    @Mock private WhatsAppService whatsAppService;
    @Mock private ConversationService conversationService;
    @Mock private TurfRepository turfRepository;

    private MockAiProvider mockAiProvider;
    private AiOrchestratorService aiOrchestratorService;

    private User customerUser;
    private Business business;
    private Conversation conversation;
    private Turf turf;

    @BeforeEach
    void setUp() {
        mockAiProvider = new MockAiProvider();
        aiOrchestratorService = new AiOrchestratorService(
                mockAiProvider,
                promptManager,
                conversationContextBuilder,
                aiToolGateway,
                whatsAppService,
                conversationService,
                turfRepository,
                new ObjectMapper()
        );

        business = Business.builder().name("Green Pitch Kolhapur").status(BusinessStatus.ACTIVE).timezone("Asia/Kolkata").build();
        business.setId(UUID.randomUUID());

        customerUser = User.builder().name("Customer").phone("+919876543210").role(UserRole.CUSTOMER).build();
        customerUser.setId(UUID.randomUUID());

        conversation = Conversation.builder().user(customerUser).business(business).status(ConversationStatus.ACTIVE).lastActivity(Instant.now()).build();
        conversation.setId(UUID.randomUUID());

        turf = Turf.builder().name("Turf A").business(business).build();
        turf.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should route customer message requesting availability to checkAvailability tool")
    void testProcessCustomerAvailabilityMessage() {
        when(promptManager.buildSystemPrompt(customerUser, business)).thenReturn("System prompt");
        when(conversationContextBuilder.buildMessageHistory(conversation)).thenReturn(List.of(Map.of("role", "user", "content", "Is slot available tomorrow?")));
        when(turfRepository.findByBusinessId(business.getId())).thenReturn(List.of(turf));
        when(aiToolGateway.checkAvailability(eq(turf.getId()), any())).thenReturn(ToolResult.success("Available slots"));

        aiOrchestratorService.processUserMessage(conversation);

        verify(aiToolGateway).checkAvailability(eq(turf.getId()), any(LocalDate.class));
        verify(whatsAppService).sendTextMessage(eq("+919876543210"), any());
        verify(conversationService).saveOutgoingMessage(eq(conversation), any(), any());
    }
}
