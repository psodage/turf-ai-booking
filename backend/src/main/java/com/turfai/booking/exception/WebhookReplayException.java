package com.turfai.booking.exception;

/**
 * Thrown when incoming webhook message timestamp is outside allowed replay window (>5 min).
 */
public class WebhookReplayException extends BaseException {

    public WebhookReplayException() {
        super(ErrorCode.WEBHOOK_REPLAY_DETECTED);
    }

    public WebhookReplayException(String message) {
        super(ErrorCode.WEBHOOK_REPLAY_DETECTED, message);
    }
}
