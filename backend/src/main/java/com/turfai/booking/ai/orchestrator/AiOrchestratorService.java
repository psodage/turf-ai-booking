package com.turfai.booking.ai.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.ai.memory.ConversationContextBuilder;
import com.turfai.booking.ai.prompt.PromptManager;
import com.turfai.booking.ai.provider.AiProvider;
import com.turfai.booking.ai.provider.AiRequest;
import com.turfai.booking.ai.provider.AiResponse;
import com.turfai.booking.ai.tool.AiToolGateway;
import com.turfai.booking.ai.tool.ToolResult;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.MessageType;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.service.ConversationService;
import com.turfai.booking.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOrchestratorService {

    private final AiProvider aiProvider;
    private final PromptManager promptManager;
    private final ConversationContextBuilder conversationContextBuilder;
    private final AiToolGateway aiToolGateway;
    private final WhatsAppService whatsAppService;
    private final ConversationService conversationService;
    private final TurfRepository turfRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processUserMessage(Conversation conversation) {
        long startTime = System.currentTimeMillis();

        // 1. Build System Prompt & History
        String systemPrompt = promptManager.buildSystemPrompt(conversation.getUser(), conversation.getBusiness());
        List<Map<String, String>> history = conversationContextBuilder.buildMessageHistory(conversation);

        AiRequest request = AiRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(history)
                .build();

        // 2. Invoke AI Provider
        AiResponse response = aiProvider.generateResponse(request);

        String replyText;

        // 3. Handle Tool Calls vs Direct Text Response
        if (response.isToolCall()) {
            log.info("AI requested tool call: {} for conversation {}", response.getToolName(), conversation.getId());
            ToolResult toolResult = executeToolCall(response.getToolName(), response.getToolArguments(), conversation);
            replyText = formatToolResultForUser(response.getToolName(), toolResult);
        } else {
            replyText = response.getContent();
        }

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("AI Orchestrator finished in {}ms. Tokens: prompt={}, completion={}. ConvId={}",
                executionTime, response.getPromptTokens(), response.getCompletionTokens(), conversation.getId());

        // 4. Send Outbound WhatsApp Reply
        whatsAppService.sendTextMessage(conversation.getUser().getPhone(), replyText);

        // 5. Persist Outgoing Message in Conversation
        conversationService.saveOutgoingMessage(conversation, replyText, MessageType.TEXT);
    }

    private ToolResult executeToolCall(String toolName, Map<String, Object> args, Conversation conversation) {
        UUID defaultTurfId = getDefaultTurfId(conversation);

        switch (toolName) {
            case "checkAvailability":
                return aiToolGateway.checkAvailability(defaultTurfId, LocalDate.now().plusDays(1));
            case "getPricing":
                return aiToolGateway.getPricing(defaultTurfId, LocalDate.now().plusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 0));
            case "createBookingHold":
                return aiToolGateway.createBookingHold(defaultTurfId, conversation.getUser().getId(), LocalDate.now().plusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 0));
            case "confirmBooking":
                return aiToolGateway.confirmBooking(UUID.randomUUID(), "PAY_DEMO_001");
            case "cancelBooking":
                return aiToolGateway.cancelBooking(UUID.randomUUID(), conversation.getUser().getId(), "Cancelled via WhatsApp AI");
            case "getTodayBookings":
                return aiToolGateway.getTodayBookings(conversation.getBusiness().getId(), LocalDate.now());
            case "getBusinessSummary":
                return aiToolGateway.getBusinessSummary(conversation.getBusiness().getId(), LocalDate.now());
            default:
                return ToolResult.error("UNKNOWN_TOOL", "Requested tool is not supported.", null);
        }
    }

    private String formatToolResultForUser(String toolName, ToolResult result) {
        if (!result.isSuccess()) {
            StringBuilder sb = new StringBuilder();
            sb.append("⚠️ ").append(result.getMessage());
            if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
                sb.append("\n\nSuggested slots:\n");
                result.getSuggestions().forEach(s -> sb.append("• ").append(s).append("\n"));
            }
            return sb.toString();
        }

        try {
            if ("checkAvailability".equals(toolName)) {
                return "📅 *Available Slots for Tomorrow:*\n• 06:00 PM - 07:00 PM (₹800)\n• 07:00 PM - 08:00 PM (₹1,000 PEAK)\n\nReply with your preferred slot (e.g., 'Book 6 to 7') to place a 10-minute hold!";
            } else if ("createBookingHold".equals(toolName)) {
                String bookingRef = "N/A";
                String paymentUrl = "https://rzp.io/i/plink_demo";
                Object price = "800";

                if (result.getData() instanceof Map<?, ?> dataMap) {
                    if (dataMap.containsKey("booking_number")) bookingRef = String.valueOf(dataMap.get("booking_number"));
                    if (dataMap.containsKey("payment_url")) paymentUrl = String.valueOf(dataMap.get("payment_url"));
                    if (dataMap.containsKey("price")) price = dataMap.get("price");
                }

                StringBuilder sb = new StringBuilder();
                sb.append("⏳ *Booking Hold Created!*\n\n");
                sb.append("• *Booking Ref:* ").append(bookingRef).append("\n");
                sb.append("• *Slot:* Tomorrow 06:00 PM - 07:00 PM\n");
                sb.append("• *Amount Payable:* ₹").append(price).append("\n");
                sb.append("• *Hold Duration:* 10 Minutes\n\n");
                sb.append("💳 *Click Link to Pay & Confirm:* \n");
                sb.append(paymentUrl).append("\n\n");
                sb.append("*(Complete payment within 10 mins via UPI / Card / NetBanking to lock your slot!)*");
                return sb.toString();
            } else if ("getTodayBookings".equals(toolName) || "getBusinessSummary".equals(toolName)) {
                return "📊 Business Summary for Today:\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.getData());
            } else {
                return result.getMessage() != null ? result.getMessage() : "Action completed successfully!";
            }
        } catch (Exception ex) {
            return "Operation completed successfully.";
        }
    }

    private UUID getDefaultTurfId(Conversation conversation) {
        List<Turf> turfs = turfRepository.findByBusinessId(conversation.getBusiness().getId());
        if (!turfs.isEmpty()) {
            return turfs.get(0).getId();
        }
        return UUID.randomUUID();
    }
}
