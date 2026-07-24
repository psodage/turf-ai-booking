package com.turfai.booking.exception;

/**
 * Thrown when attempting to confirm or operate on an expired booking hold.
 */
public class HoldExpiredException extends BaseException {

    public HoldExpiredException() {
        super(ErrorCode.HOLD_EXPIRED);
    }

    public HoldExpiredException(String message) {
        super(ErrorCode.HOLD_EXPIRED, message);
    }
}
