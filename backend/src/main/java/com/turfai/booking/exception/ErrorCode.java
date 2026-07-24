package com.turfai.booking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Standard Error Codes mapped to HTTP status codes and user-friendly error messages.
 * Matches AI tool error codes and system-wide error contracts.
 */
@Getter
public enum ErrorCode {

    // Validation & General
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "The request payload is invalid or malformed."),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Request validation failed."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again."),

    // Business & Booking Rules
    SLOT_UNAVAILABLE(HttpStatus.CONFLICT, "The requested slot is no longer available."),
    HOLD_EXPIRED(HttpStatus.CONFLICT, "The booking hold has expired."),
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, "Booking not found."),
    CANCELLATION_DENIED(HttpStatus.UNPROCESSABLE_ENTITY, "Cancellation is not allowed for this booking."),
    OUTSIDE_OPERATING_HOURS(HttpStatus.UNPROCESSABLE_ENTITY, "Requested slot is outside operating hours."),

    // Payment
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "Payment signature or payload verification failed."),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "Payment has already been processed for this booking."),

    // Security & Webhooks
    UNAUTHORIZED_BUSINESS_ACCESS(HttpStatus.FORBIDDEN, "Access to requested business resource is forbidden."),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "Invalid webhook signature."),
    WEBHOOK_REPLAY_DETECTED(HttpStatus.BAD_REQUEST, "Expired or replayed webhook message received.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
