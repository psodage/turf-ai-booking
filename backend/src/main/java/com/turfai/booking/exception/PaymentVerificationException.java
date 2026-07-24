package com.turfai.booking.exception;

/**
 * Thrown when payment gateway webhook or response verification fails.
 */
public class PaymentVerificationException extends BaseException {

    public PaymentVerificationException() {
        super(ErrorCode.PAYMENT_VERIFICATION_FAILED);
    }

    public PaymentVerificationException(String message) {
        super(ErrorCode.PAYMENT_VERIFICATION_FAILED, message);
    }
}
