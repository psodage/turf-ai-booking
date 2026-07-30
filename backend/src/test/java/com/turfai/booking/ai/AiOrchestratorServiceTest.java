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
                new ObjectMapper(),
                new com.turfai.booking.ai.language.LanguageDetector(),
                new com.turfai.booking.ai.language.MultilingualMessageFormatter()
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
        when(promptManager.buildSystemPrompt(eq(customerUser), eq(business), any())).thenReturn("System prompt");
        when(conversationContextBuilder.buildMessageHistory(conversation)).thenReturn(List.of(Map.of("role", "user", "content", "Is slot available tomorrow?")));
        when(turfRepository.findByBusinessId(business.getId())).thenReturn(List.of(turf));
        when(aiToolGateway.checkAvailability(eq(turf.getId()), any())).thenReturn(ToolResult.success("Available slots"));

        aiOrchestratorService.processUserMessage(conversation);

        verify(aiToolGateway).checkAvailability(eq(turf.getId()), any(LocalDate.class));
        verify(whatsAppService).sendTextMessage(eq("+919876543210"), any());
        verify(conversationService).saveOutgoingMessage(eq(conversation), any(), any());
    }

    @Test
    @DisplayName("Should route greeting or menu prompt to interactive WhatsApp menu list")
    void testProcessMenuMessage() {
        when(promptManager.buildSystemPrompt(eq(customerUser), eq(business), any())).thenReturn("System prompt");
        when(conversationContextBuilder.buildMessageHistory(conversation)).thenReturn(List.of(Map.of("role", "user", "content", "Hi")));

        aiOrchestratorService.processUserMessage(conversation);

        verify(whatsAppService).sendListMessage(eq("+919876543210"), eq("👋 Welcome to Green Pitch Kolhapur"), eq("Please select an option below or type your query."), eq("Menu Options"), any());
        verify(conversationService).saveOutgoingMessage(eq(conversation), any(), any());
    }

    @Test
    @DisplayName("Should route location request to location tool and send location message")
    void testProcessLocationMessage() {
        business.setLatitude(16.6946);
        business.setLongitude(74.2179);
        business.setAddress("Near Rankala Lake, Ring Road, Kolhapur");

        when(promptManager.buildSystemPrompt(eq(customerUser), eq(business), any())).thenReturn("System prompt");
        when(conversationContextBuilder.buildMessageHistory(conversation)).thenReturn(List.of(Map.of("role", "user", "content", "Where is your location?")));
        when(aiToolGateway.getLocation(business)).thenReturn(ToolResult.success("Location retrieved", Map.of(
                "name", business.getName(),
                "address", business.getAddress(),
                "latitude", business.getLatitude(),
                "longitude", business.getLongitude(),
                "has_native_location", true
        )));

        aiOrchestratorService.processUserMessage(conversation);

        verify(whatsAppService).sendLocationMessage(eq("+919876543210"), eq(16.6946), eq(74.2179), eq("Green Pitch Kolhapur"), eq("Near Rankala Lake, Ring Road, Kolhapur"));
        verify(conversationService).saveOutgoingMessage(eq(conversation), any(), any());
    }

    @Test
    @DisplayName("Should route view booking request to getUserBookings tool")
    void testProcessViewBookingMessage() {
        when(promptManager.buildSystemPrompt(eq(customerUser), eq(business), any())).thenReturn("System prompt");
        when(conversationContextBuilder.buildMessageHistory(conversation)).thenReturn(List.of(Map.of("role", "user", "content", "View my booking")));
        when(aiToolGateway.getUserBookings(customerUser, null)).thenReturn(ToolResult.success("Bookings found", Map.of(
                "found", true,
                "count", 1,
                "bookings", List.of(Map.of(
                        "booking_id", "BK-2026-00001",
                        "date", "2026-08-01",
                        "time_slot", "18:00 - 19:00",
                        "turf_name", "Green Pitch Main Turf",
                        "status", "CONFIRMED",
                        "amount_paid", 800
                ))
        )));

        aiOrchestratorService.processUserMessage(conversation);

        verify(whatsAppService).sendTextMessage(eq("+919876543210"), any());
        verify(conversationService).saveOutgoingMessage(eq(conversation), any(), any());
    }
}
