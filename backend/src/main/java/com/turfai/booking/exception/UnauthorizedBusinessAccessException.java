package com.turfai.booking.exception;

/**
 * Thrown when multi-tenant isolation rules are violated.
 */
public class UnauthorizedBusinessAccessException extends BaseException {

    public UnauthorizedBusinessAccessException() {
        super(ErrorCode.UNAUTHORIZED_BUSINESS_ACCESS);
    }

    public UnauthorizedBusinessAccessException(String message) {
        super(ErrorCode.UNAUTHORIZED_BUSINESS_ACCESS, message);
    }
}
