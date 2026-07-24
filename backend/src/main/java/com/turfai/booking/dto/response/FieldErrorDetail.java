package com.turfai.booking.dto.response;

/**
 * Details of an individual field validation error.
 */
public record FieldErrorDetail(
    String field,
    String message
) {}
