package com.turfai.booking.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiProvider implements AiProvider {

    @Override
    public AiResponse generateResponse(AiRequest request) {
        log.info("MockAiProvider processing request with {} messages", request.getMessages() != null ? request.getMessages().size() : 0);

        String lastMessage = "";
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            lastMessage = request.getMessages().get(request.getMessages().size() - 1).getOrDefault("content", "").toLowerCase();
        }

        // View Booking intent (or phone number input for lookup)
        if (lastMessage.contains("view my booking") || lastMessage.contains("my booking") || lastMessage.contains("view_booking") 
                || lastMessage.contains("view booking") || lastMessage.contains("my bookings")
                || lastMessage.contains("meri booking") || lastMessage.contains("maji booking") || lastMessage.contains("माझी बुकिंग") || lastMessage.contains("मेरी बुकिंग")
                || lastMessage.matches(".*\\b[0-9]{10}\\b.*")) {
            String extractedPhone = "";
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b[0-9]{10}\\b").matcher(lastMessage);
            if (matcher.find()) {
                extractedPhone = matcher.group();
            }
            Map<String, Object> args = extractedPhone.isEmpty() ? Map.of() : Map.of("phone", extractedPhone);
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("getUserBookings")
                    .toolArguments(args)
                    .promptTokens(100)
                    .completionTokens(40)
                    .build();
        }

        // Location & Map intent
        if (lastMessage.contains("location") || lastMessage.contains("address") || lastMessage.contains("map") || lastMessage.contains("where") 
                || lastMessage.contains("location_map") || lastMessage.contains("लोकेशन") || lastMessage.contains("स्थान") || lastMessage.contains("पत्ता") || lastMessage.contains("पता")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("getLocation")
                    .toolArguments(Map.of())
                    .promptTokens(90)
                    .completionTokens(35)
                    .build();
        }

        if (lastMessage.contains("hold") || lastMessage.contains("6 to 7") || lastMessage.contains("6:00") || lastMessage.contains("06:00")
                || lastMessage.contains("7 to 8") || lastMessage.contains("07:00") || lastMessage.contains("first") || lastMessage.contains("second")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("createBookingHold")
                    .toolArguments(Map.of(
                            "date", LocalDate.now().plusDays(1).toString(),
                            "startTime", "18:00:00",
                            "endTime", "19:00:00"
                    ))
                    .promptTokens(130)
                    .completionTokens(50)
                    .build();
        }

        if (lastMessage.contains("price") || lastMessage.contains("rate") || lastMessage.contains("cost") || lastMessage.contains("charge") 
                || lastMessage.contains("pricing") || lastMessage.contains("दर") || lastMessage.contains("दरपत्रक") || lastMessage.contains("रेट") || lastMessage.contains("मूल्य")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("getPricing")
                    .toolArguments(Map.of())
                    .promptTokens(90)
                    .completionTokens(40)
                    .build();
        }

        if (lastMessage.contains("availability") || lastMessage.contains("slot") || lastMessage.contains("available") 
                || lastMessage.contains("book") || lastMessage.contains("reserve") || lastMessage.contains("turf")
                || lastMessage.contains("time") || lastMessage.contains("play") || lastMessage.contains("check_availability")
                || lastMessage.contains("स्लॉट") || lastMessage.contains("बुकिंग") || lastMessage.contains("बुक") || lastMessage.contains("उद्या") || lastMessage.contains("कल")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("checkAvailability")
                    .toolArguments(Map.of("date", LocalDate.now().plusDays(1).toString()))
                    .promptTokens(120)
                    .completionTokens(45)
                    .build();
        }

        if (lastMessage.contains("today's bookings") || lastMessage.contains("today bookings") || lastMessage.contains("revenue") || lastMessage.contains("summary")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("getTodayBookings")
                    .toolArguments(Map.of("date", LocalDate.now().toString()))
                    .promptTokens(100)
                    .completionTokens(30)
                    .build();
        }

        if (lastMessage.contains("cancel") || lastMessage.contains("रद्द") || lastMessage.contains("cancel_booking")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("cancelBooking")
                    .toolArguments(Map.of("reason", "Customer requested cancellation"))
                    .promptTokens(110)
                    .completionTokens(40)
                    .build();
        }

        // Return tool call for interactive menu display
        return AiResponse.builder()
                .isToolCall(true)
                .toolName("showMenu")
                .toolArguments(Map.of())
                .promptTokens(85)
                .completionTokens(40)
                .build();
    }
}
