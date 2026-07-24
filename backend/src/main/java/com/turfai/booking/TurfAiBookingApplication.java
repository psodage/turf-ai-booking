package com.turfai.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Turf AI Booking backend.
 *
 * <p>The application is a WhatsApp-first AI booking system for football turf businesses.
 * It receives WhatsApp webhooks, routes them through an AI agent, and manages bookings,
 * payments, and reporting via Spring Boot + PostgreSQL.
 *
 * <p>Key design principle: "The AI talks. The Backend decides. The Database stores."
 *
 * <p>{@code @EnableScheduling} activates hold expiry cleanup, booking reminders,
 * and daily report generation — all implemented as {@code @Scheduled} tasks.
 */
@SpringBootApplication
@EnableScheduling
public class TurfAiBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurfAiBookingApplication.class, args);
    }
}
