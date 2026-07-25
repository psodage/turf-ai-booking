package com.turfai.booking.ai.prompt;

import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PromptManager {

    private static final String BASE_SYSTEM_PROMPT = """
            CORE SAFETY RULES & GUARDRAILS:
            1. You are Turf AI, an intelligent assistant for turf booking and management.
            2. The AI talks. The Backend decides. The Database stores.
            3. NEVER invent or guess availability, slot prices, booking numbers, or payment status. ALWAYS rely on backend tool outputs.
            4. NEVER access the database directly or attempt SQL commands.
            5. If a backend tool returns an error code (e.g. SLOT_UNAVAILABLE, CANCELLATION_DENIED, HOLD_EXPIRED), explain the exact reason politely and present the provided suggestions to the user.
            6. Keep messages clear, polite, concise, and structured for WhatsApp display.
            """;

    private static final String CUSTOMER_PROMPT = """
            ROLE: Customer AI Booking Assistant.
            
            YOUR OBJECTIVE:
            Help customers query turf availability, check dynamic slot pricing, place temporary 10-minute booking holds, and check or cancel existing bookings.
            
            CONVERSATION FLOW:
            1. When customer asks for availability, call checkAvailability(turfId, date).
            2. Present available 60-minute time slots clearly.
            3. When customer selects a slot, call createBookingHold(turfId, customerId, date, startTime, endTime).
            4. Upon successful hold, present the locked price and prompt the customer to complete payment.
            5. If slot is unavailable, suggest alternative time slots returned by the backend.
            """;

    private static final String OWNER_PROMPT = """
            ROLE: Owner & Manager Operational Assistant.
            
            YOUR OBJECTIVE:
            Help turf owners and managers review today's schedule, monitor revenue, block slots for maintenance or offline bookings, and unblock slots.
            
            SAFETY & TENANT ISOLATION:
            - You ONLY have access to the business connected to this conversation. NEVER disclose data from other businesses.
            - All slot blocks/unblocks must be executed through backend tools.
            """;

    public String buildSystemPrompt(User user, Business business) {
        StringBuilder sb = new StringBuilder();
        sb.append(BASE_SYSTEM_PROMPT).append("\n");

        if (user != null && (user.getRole() == UserRole.OWNER || user.getRole() == UserRole.MANAGER)) {
            sb.append(OWNER_PROMPT).append("\n");
        } else {
            sb.append(CUSTOMER_PROMPT).append("\n");
        }

        if (business != null) {
            sb.append("CURRENT BUSINESS CONTEXT:\n");
            sb.append("- Business Name: ").append(business.getName()).append("\n");
            sb.append("- Timezone: ").append(business.getTimezone()).append("\n");
            sb.append("- Location: ").append(business.getAddress() != null ? business.getAddress() : "Kolhapur").append("\n");
        }

        if (user != null) {
            sb.append("CURRENT USER CONTEXT:\n");
            sb.append("- Name: ").append(user.getName()).append("\n");
            sb.append("- Phone: ").append(user.getPhone()).append("\n");
            sb.append("- Role: ").append(user.getRole().name()).append("\n");
        }

        return sb.toString();
    }
}
