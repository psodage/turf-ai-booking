package com.turfai.booking.dto.response;

import java.util.List;

/**
 * Standard top-level Error Response wrapper returned by GlobalExceptionHandler and AI Tools.
 */
public record ErrorResponse(
    boolean success,
    ErrorDetails error
) {
    public static ErrorResponse of(String code, String message, String correlationId) {
        return new ErrorResponse(false, new ErrorDetails(code, message, correlationId, null, null));
    }

    public static ErrorResponse of(String code, String message, String correlationId, List<FieldErrorDetail> fields) {
        return new ErrorResponse(false, new ErrorDetails(code, message, correlationId, fields, null));
    }

    public static ErrorResponse of(String code, String message, String correlationId, List<FieldErrorDetail> fields, List<String> suggestions) {
        return new ErrorResponse(false, new ErrorDetails(code, message, correlationId, fields, suggestions));
    }
}
