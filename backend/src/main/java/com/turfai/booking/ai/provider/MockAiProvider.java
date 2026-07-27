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

        if (lastMessage.contains("availability") || lastMessage.contains("slot") || lastMessage.contains("available") 
                || lastMessage.contains("book") || lastMessage.contains("reserve") || lastMessage.contains("turf")
                || lastMessage.contains("time") || lastMessage.contains("play") || lastMessage.matches(".*\\d.*")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("checkAvailability")
                    .toolArguments(Map.of("date", LocalDate.now().plusDays(1).toString()))
                    .promptTokens(120)
                    .completionTokens(45)
                    .build();
        }

        if (lastMessage.contains("price") || lastMessage.contains("rate") || lastMessage.contains("cost") || lastMessage.contains("charge")) {
            return AiResponse.builder()
                    .isToolCall(false)
                    .content("⚽ *Green Pitch Kolhapur Pricing:*\n\n• Standard Hours (6 AM - 5 PM): ₹800/hr\n• Peak Hours (5 PM - 11 PM): ₹1,000/hr\n\nReply with your preferred date & time to check available slots!")
                    .promptTokens(90)
                    .completionTokens(40)
                    .build();
        }

        if (lastMessage.contains("location") || lastMessage.contains("address") || lastMessage.contains("map") || lastMessage.contains("where")) {
            return AiResponse.builder()
                    .isToolCall(false)
                    .content("📍 *Green Pitch Kolhapur*\nAddress: Near Rankala Lake, Ring Road, Kolhapur, Maharashtra (416012)\nGoogle Maps: https://maps.google.com/?q=Rankala+Kolhapur")
                    .promptTokens(90)
                    .completionTokens(35)
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

        if (lastMessage.contains("cancel")) {
            return AiResponse.builder()
                    .isToolCall(true)
                    .toolName("cancelBooking")
                    .toolArguments(Map.of("reason", "Customer requested cancellation"))
                    .promptTokens(110)
                    .completionTokens(40)
                    .build();
        }

        // Standard text response for greetings
        return AiResponse.builder()
                .isToolCall(false)
                .content("👋 Hello! Welcome to Green Pitch Kolhapur.\n\nYou can ask me:\n1. 📅 *Check availability* (e.g., 'book slot for tomorrow')\n2. 💰 *Check pricing* (e.g., 'what are the rates?')\n3. 📍 *Location & Map*\n4. ❌ *Cancel booking*")
                .promptTokens(85)
                .completionTokens(40)
                .build();
    }
}
