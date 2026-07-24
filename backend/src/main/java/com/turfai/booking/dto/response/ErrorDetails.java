package com.turfai.booking.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Inner error payload details for structured JSON responses.
 */
public record ErrorDetails(
    String code,
    String message,
    Instant timestamp,
    String correlationId,
    List<FieldErrorDetail> fields,
    List<String> suggestions
) {
    public ErrorDetails(String code, String message, String correlationId, List<FieldErrorDetail> fields, List<String> suggestions) {
        this(code, message, Instant.now(), correlationId, fields, suggestions);
    }
}
