package com.turfai.booking.ai.orchestrator;

import com.turfai.booking.ai.language.LanguageDetector;
import com.turfai.booking.ai.language.MultilingualMessageFormatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.ai.memory.ConversationContextBuilder;
import com.turfai.booking.ai.prompt.PromptManager;
import com.turfai.booking.ai.provider.AiProvider;
import com.turfai.booking.ai.provider.AiRequest;
import com.turfai.booking.ai.provider.AiResponse;
import com.turfai.booking.ai.tool.AiToolGateway;
import com.turfai.booking.ai.tool.ToolResult;
import com.turfai.booking.entity.BlockReason;
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
    private final LanguageDetector languageDetector;
    private final MultilingualMessageFormatter multilingualMessageFormatter;

    @Transactional
    public void processUserMessage(Conversation conversation) {
        long startTime = System.currentTimeMillis();

        List<Map<String, String>> history = conversationContextBuilder.buildMessageHistory(conversation);

        // 1. Detect & Update Language State
        String lastUserMsg = "";
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                Map<String, String> m = history.get(i);
                if ("user".equals(m.get("role"))) {
                    lastUserMsg = m.getOrDefault("content", "");
                    break;
                }
            }
        }

        String currentLang = conversation.getPreferredLanguage();
        String detectedLang = languageDetector.detectLanguage(lastUserMsg, currentLang);
        conversation.setPreferredLanguage(detectedLang);

        // 2. Build System Prompt & History
        String systemPrompt = promptManager.buildSystemPrompt(conversation.getUser(), conversation.getBusiness(), detectedLang);

        AiRequest request = AiRequest.builder()
                .systemPrompt(systemPrompt)
                .messages(history)
                .build();

        // 3. Invoke AI Provider
        AiResponse response = aiProvider.generateResponse(request);

        String replyText;

        // 4. Handle Tool Calls vs Direct Text Response
        if (response.isToolCall()) {
            log.info("AI requested tool call: {} for conversation {} in language {}", response.getToolName(), conversation.getId(), detectedLang);
            if ("showMenu".equals(response.getToolName())) {
                sendInteractiveMenu(conversation, detectedLang);
                return;
            } else if ("getLocation".equals(response.getToolName())) {
                sendLocationResponse(conversation, detectedLang);
                return;
            } else if ("getUserBookings".equals(response.getToolName())) {
                String inputPhone = (response.getToolArguments() != null && response.getToolArguments().containsKey("phone")) 
                        ? String.valueOf(response.getToolArguments().get("phone")) : null;
                sendUserBookingsResponse(conversation, inputPhone, detectedLang);
                return;
            }
            ToolResult toolResult = executeToolCall(response.getToolName(), response.getToolArguments(), conversation);
            replyText = formatToolResultForUser(response.getToolName(), toolResult, detectedLang);
        } else {
            replyText = response.getContent();
        }

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("AI Orchestrator finished in {}ms. Tokens: prompt={}, completion={}. ConvId={}",
                executionTime, response.getPromptTokens(), response.getCompletionTokens(), conversation.getId());

        // 5. Send Outbound WhatsApp Reply
        whatsAppService.sendTextMessage(conversation.getUser().getPhone(), replyText);

        // 6. Persist Outgoing Message in Conversation
        conversationService.saveOutgoingMessage(conversation, replyText, MessageType.TEXT);
    }

    private void sendInteractiveMenu(Conversation conversation, String lang) {
        MultilingualMessageFormatter.MenuConfig menuConfig = multilingualMessageFormatter.getMenuConfig(lang);

        whatsAppService.sendListMessage(
                conversation.getUser().getPhone(),
                menuConfig.header(),
                menuConfig.body(),
                menuConfig.buttonText(),
                menuConfig.sections()
        );
        conversationService.saveOutgoingMessage(conversation, menuConfig.header() + "\n" + menuConfig.body(), MessageType.LIST);
        log.info("Sent interactive WhatsApp menu list to customer {} in language {}", conversation.getUser().getPhone(), lang);
    }

    private void sendLocationResponse(Conversation conversation, String lang) {
        ToolResult toolResult = aiToolGateway.getLocation(conversation.getBusiness());
        String name = conversation.getBusiness() != null ? conversation.getBusiness().getName() : "Green Pitch Kolhapur";
        String address = "Near Rankala Lake, Ring Road, Kolhapur, Maharashtra (416012)";

        if (toolResult.isSuccess() && toolResult.getData() instanceof Map<?, ?> dataMap) {
            Boolean hasNative = (Boolean) dataMap.get("has_native_location");
            if (Boolean.TRUE.equals(hasNative)) {
                double lat = ((Number) dataMap.get("latitude")).doubleValue();
                double lng = ((Number) dataMap.get("longitude")).doubleValue();
                if (dataMap.containsKey("name")) name = String.valueOf(dataMap.get("name"));
                if (dataMap.containsKey("address")) address = String.valueOf(dataMap.get("address"));

                whatsAppService.sendLocationMessage(conversation.getUser().getPhone(), lat, lng, name, address);
                String summaryMsg = multilingualMessageFormatter.formatLocationSummary(lang, name, address);
                whatsAppService.sendTextMessage(conversation.getUser().getPhone(), summaryMsg);
                conversationService.saveOutgoingMessage(conversation, summaryMsg, MessageType.TEXT);
                log.info("Sent native WhatsApp location message for business {} to user {} in {}", name, conversation.getUser().getPhone(), lang);
                return;
            }
        }

        // Fallback location text message
        String fallbackMsg = multilingualMessageFormatter.formatLocationSummary(lang, name, address);
        whatsAppService.sendTextMessage(conversation.getUser().getPhone(), fallbackMsg);
        conversationService.saveOutgoingMessage(conversation, fallbackMsg, MessageType.TEXT);
    }

    private void sendUserBookingsResponse(Conversation conversation, String inputPhone, String lang) {
        ToolResult toolResult = aiToolGateway.getUserBookings(conversation.getUser(), inputPhone);
        String messageText;

        if (toolResult.isSuccess() && toolResult.getData() instanceof Map<?, ?> dataMap && Boolean.TRUE.equals(dataMap.get("found"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bookings = (List<Map<String, Object>>) dataMap.get("bookings");
            messageText = multilingualMessageFormatter.formatBookingDetails(lang, bookings);
        } else {
            messageText = multilingualMessageFormatter.formatNoBookingFound(lang);
        }

        whatsAppService.sendTextMessage(conversation.getUser().getPhone(), messageText);
        conversationService.saveOutgoingMessage(conversation, messageText, MessageType.TEXT);
        log.info("Sent user booking lookup response to {} in language {}", conversation.getUser().getPhone(), lang);
    }

    private ToolResult executeToolCall(String toolName, Map<String, Object> args, Conversation conversation) {
        UUID defaultTurfId = parseUuidArg(args != null ? args.get("turfId") : null, getDefaultTurfId(conversation));
        LocalDate date = parseDateArg(args != null ? args.get("date") : null, LocalDate.now().plusDays(1));
        LocalTime startTime = parseTimeArg(args != null ? args.get("startTime") : null, LocalTime.of(18, 0));
        LocalTime endTime = parseTimeArg(args != null ? args.get("endTime") : null, startTime.plusHours(1));

        switch (toolName) {
            case "checkAvailability":
                return aiToolGateway.checkAvailability(defaultTurfId, date);
            case "getPricing":
                return aiToolGateway.getPricing(defaultTurfId, date, startTime, endTime);
            case "createBookingHold":
                return aiToolGateway.createBookingHold(defaultTurfId, conversation.getUser().getId(), date, startTime, endTime);
            case "confirmBooking":
                return aiToolGateway.confirmBooking(UUID.randomUUID(), "PAY_DEMO_001");
            case "cancelBooking":
                return aiToolGateway.cancelBooking(UUID.randomUUID(), conversation.getUser().getId(), "Cancelled via WhatsApp AI");
            case "getTodayBookings":
                return aiToolGateway.getTodayBookings(conversation.getBusiness().getId(), LocalDate.now());
            case "getBusinessSummary":
                return aiToolGateway.getBusinessSummary(conversation.getBusiness().getId(), LocalDate.now());
            case "blockSlot":
                BlockReason reason = BlockReason.MAINTENANCE;
                if (args != null && args.containsKey("reason")) {
                    try { reason = BlockReason.valueOf(String.valueOf(args.get("reason")).toUpperCase()); } catch (Exception e) {}
                }
                return aiToolGateway.blockSlot(defaultTurfId, date, startTime, endTime, reason, conversation.getUser().getId());
            case "unblockSlot":
                UUID blockedId = parseUuidArg(args != null ? args.get("blockedSlotId") : null, UUID.randomUUID());
                return aiToolGateway.unblockSlot(blockedId, conversation.getUser().getId());
            default:
                return ToolResult.error("UNKNOWN_TOOL", "Requested tool is not supported.", null);
        }
    }

    private LocalDate parseDateArg(Object val, LocalDate fallback) {
        if (val == null) return fallback;
        try {
            String s = String.valueOf(val).trim();
            if ("today".equalsIgnoreCase(s)) return LocalDate.now();
            if ("tomorrow".equalsIgnoreCase(s)) return LocalDate.now().plusDays(1);
            return LocalDate.parse(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private LocalTime parseTimeArg(Object val, LocalTime fallback) {
        if (val == null) return fallback;
        try {
            String s = String.valueOf(val).trim();
            if (s.length() == 5) s += ":00";
            return LocalTime.parse(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private UUID parseUuidArg(Object val, UUID fallback) {
        if (val == null) return fallback;
        try {
            return UUID.fromString(String.valueOf(val).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String formatToolResultForUser(String toolName, ToolResult result, String lang) {
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
                return multilingualMessageFormatter.formatAvailability(lang);
            } else if ("getPricing".equals(toolName)) {
                return multilingualMessageFormatter.formatPricing(lang);
            } else if ("cancelBooking".equals(toolName)) {
                return multilingualMessageFormatter.formatCancellation(lang);
            } else if ("createBookingHold".equals(toolName)) {
                String bookingRef = "N/A";
                String paymentUrl = "https://rzp.io/i/plink_demo";
                Object price = "800";

                if (result.getData() instanceof Map<?, ?> dataMap) {
                    if (dataMap.containsKey("booking_number")) bookingRef = String.valueOf(dataMap.get("booking_number"));
                    if (dataMap.containsKey("payment_url")) paymentUrl = String.valueOf(dataMap.get("payment_url"));
                    if (dataMap.containsKey("price")) price = dataMap.get("price");
                }

                return multilingualMessageFormatter.formatHoldCreated(lang, bookingRef, paymentUrl, price);
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
