package com.turfai.booking.exception;

/**
 * Thrown when duplicate payment event is received for an already confirmed booking.
 */
public class DuplicatePaymentException extends BaseException {

    public DuplicatePaymentException() {
        super(ErrorCode.DUPLICATE_PAYMENT);
    }

    public DuplicatePaymentException(String message) {
        super(ErrorCode.DUPLICATE_PAYMENT, message);
    }
}
