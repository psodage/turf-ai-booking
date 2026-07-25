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

        if (lastMessage.contains("availability") || lastMessage.contains("slot") || lastMessage.contains("available")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("checkAvailability")
                    .toolArguments(Map.of("date", LocalDate.now().plusDays(1).toString()))
                    .promptTokens(120)
                    .completionTokens(45)
                    .build();
        }

        if (lastMessage.contains("today's bookings") || lastMessage.contains("today bookings") || lastMessage.contains("revenue")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("getTodayBookings")
                    .toolArguments(Map.of("date", LocalDate.now().toString()))
                    .promptTokens(100)
                    .completionTokens(30)
                    .build();
        }

        if (lastMessage.contains("cancel")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("cancelBooking")
                    .toolArguments(Map.of("reason", "Customer requested cancellation"))
                    .promptTokens(110)
                    .completionTokens(40)
                    .build();
        }

        // Standard text response
        return AiResponse.builder()
                .isToolCall(false)
                .content("Hello! Welcome to Green Pitch Kolhapur. How can I help you book or manage your turf slot today?")
                .promptTokens(85)
                .completionTokens(30)
                .build();
    }
}
