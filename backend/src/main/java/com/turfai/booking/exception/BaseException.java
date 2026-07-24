package com.turfai.booking.exception;

import lombok.Getter;

import java.util.List;

/**
 * Root RuntimeException for all domain-specific application exceptions.
 * Encapsulates ErrorCode, customizable message, suggestions, and field errors.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> suggestions;

    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.suggestions = null;
    }

    protected BaseException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.suggestions = null;
    }

    protected BaseException(ErrorCode errorCode, String customMessage, List<String> suggestions) {
        super(customMessage);
        this.errorCode = errorCode;
        this.suggestions = suggestions;
    }
}
