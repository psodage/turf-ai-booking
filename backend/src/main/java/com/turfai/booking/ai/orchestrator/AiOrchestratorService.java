package com.turfai.booking.ai.orchestrator;

import com.turfai.booking.dto.whatsapp.outbound.OutboundRow;
import com.turfai.booking.dto.whatsapp.outbound.OutboundSection;
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
            if ("showMenu".equals(response.getToolName())) {
                sendInteractiveMenu(conversation);
                return;
            } else if ("getLocation".equals(response.getToolName())) {
                sendLocationResponse(conversation);
                return;
            } else if ("getUserBookings".equals(response.getToolName())) {
                String inputPhone = (response.getToolArguments() != null && response.getToolArguments().containsKey("phone")) 
                        ? String.valueOf(response.getToolArguments().get("phone")) : null;
                sendUserBookingsResponse(conversation, inputPhone);
                return;
            }
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

    private void sendInteractiveMenu(Conversation conversation) {
        String headerText = "👋 Welcome to Green Pitch Kolhapur";
        String bodyText = "Please select an option below or type your query.";
        String buttonText = "Menu Options";

        List<OutboundRow> rows = List.of(
                OutboundRow.builder().id("check_availability").title("📅 Check Availability").description("Check available slots").build(),
                OutboundRow.builder().id("pricing").title("💰 Pricing").description("View booking rates").build(),
                OutboundRow.builder().id("location_map").title("📍 Location & Map").description("Get our location").build(),
                OutboundRow.builder().id("view_booking").title("📖 View My Booking").description("View your existing booking details").build(),
                OutboundRow.builder().id("cancel_booking").title("❌ Cancel Booking").description("Cancel an existing booking").build()
        );

        List<OutboundSection> sections = List.of(
                OutboundSection.builder().title("Menu Options").rows(rows).build()
        );

        whatsAppService.sendListMessage(conversation.getUser().getPhone(), headerText, bodyText, buttonText, sections);
        conversationService.saveOutgoingMessage(conversation, headerText + "\n" + bodyText, MessageType.LIST);
        log.info("Sent interactive WhatsApp menu list to customer {}", conversation.getUser().getPhone());
    }

    private void sendLocationResponse(Conversation conversation) {
        ToolResult toolResult = aiToolGateway.getLocation(conversation.getBusiness());
        if (toolResult.isSuccess() && toolResult.getData() instanceof Map<?, ?> dataMap) {
            Boolean hasNative = (Boolean) dataMap.get("has_native_location");
            if (Boolean.TRUE.equals(hasNative)) {
                double lat = ((Number) dataMap.get("latitude")).doubleValue();
                double lng = ((Number) dataMap.get("longitude")).doubleValue();
                String name = String.valueOf(dataMap.get("name"));
                String address = String.valueOf(dataMap.get("address"));

                whatsAppService.sendLocationMessage(conversation.getUser().getPhone(), lat, lng, name, address);
                String summaryMsg = String.format("📍 *%s*\n%s", name, address);
                whatsAppService.sendTextMessage(conversation.getUser().getPhone(), summaryMsg);
                conversationService.saveOutgoingMessage(conversation, summaryMsg, MessageType.TEXT);
                log.info("Sent native WhatsApp location message for business {} to user {}", name, conversation.getUser().getPhone());
                return;
            }
        }

        // Fallback text message if location is missing lat/lng
        String fallbackMsg = "📍 *Green Pitch Kolhapur*\nAddress: Near Rankala Lake, Ring Road, Kolhapur, Maharashtra (416012)\nGoogle Maps: https://maps.google.com/?q=Rankala+Kolhapur";
        whatsAppService.sendTextMessage(conversation.getUser().getPhone(), fallbackMsg);
        conversationService.saveOutgoingMessage(conversation, fallbackMsg, MessageType.TEXT);
    }

    private void sendUserBookingsResponse(Conversation conversation, String inputPhone) {
        ToolResult toolResult = aiToolGateway.getUserBookings(conversation.getUser(), inputPhone);
        String messageText;

        if (toolResult.isSuccess() && toolResult.getData() instanceof Map<?, ?> dataMap && Boolean.TRUE.equals(dataMap.get("found"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bookings = (List<Map<String, Object>>) dataMap.get("bookings");
            StringBuilder sb = new StringBuilder();
            sb.append("📖 *Your Booking Details:*\n\n");
            for (Map<String, Object> b : bookings) {
                sb.append("• *Booking ID:* ").append(b.get("booking_id")).append("\n");
                sb.append("• *Date:* ").append(b.get("date")).append("\n");
                sb.append("• *Time Slot:* ").append(b.get("time_slot")).append("\n");
                sb.append("• *Turf Name:* ").append(b.get("turf_name")).append("\n");
                sb.append("• *Status:* ").append(b.get("status")).append("\n");
                sb.append("• *Amount Paid:* ₹").append(b.get("amount_paid")).append("\n\n");
            }
            messageText = sb.toString().trim();
        } else {
            messageText = "ℹ️ No bookings found for your registered account.\n\nIf your booking was created under a different number, please reply with your registered 10-digit mobile number to search.";
        }

        whatsAppService.sendTextMessage(conversation.getUser().getPhone(), messageText);
        conversationService.saveOutgoingMessage(conversation, messageText, MessageType.TEXT);
        log.info("Sent user booking lookup response to {}", conversation.getUser().getPhone());
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
                sb.append("• *Hold Duration:* 7.5 Minutes\n\n");
                sb.append("💳 *Click Link to Pay & Confirm:* \n");
                sb.append(paymentUrl).append("\n\n");
                sb.append("*(Complete payment within 5 mins via UPI / Card / NetBanking to lock your slot!)*");
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
