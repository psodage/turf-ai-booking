package com.turfai.booking.exception;

/**
 * Thrown when incoming webhook HMAC signature check fails.
 */
public class WebhookSignatureException extends BaseException {

    public WebhookSignatureException() {
        super(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
    }

    public WebhookSignatureException(String message) {
        super(ErrorCode.INVALID_WEBHOOK_SIGNATURE, message);
    }
}
