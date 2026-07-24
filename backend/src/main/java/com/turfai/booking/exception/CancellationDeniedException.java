package com.turfai.booking.exception;

/**
 * Thrown when cancellation request violates policy (e.g. within 2 hours of start time).
 */
public class CancellationDeniedException extends BaseException {

    public CancellationDeniedException() {
        super(ErrorCode.CANCELLATION_DENIED);
    }

    public CancellationDeniedException(String message) {
        super(ErrorCode.CANCELLATION_DENIED, message);
    }
}
