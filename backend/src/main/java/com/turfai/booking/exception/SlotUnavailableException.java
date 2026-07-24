package com.turfai.booking.exception;

import java.util.List;

/**
 * Thrown when a requested turf slot is already booked, on hold, or blocked.
 */
public class SlotUnavailableException extends BaseException {

    public SlotUnavailableException() {
        super(ErrorCode.SLOT_UNAVAILABLE);
    }

    public SlotUnavailableException(String message) {
        super(ErrorCode.SLOT_UNAVAILABLE, message);
    }

    public SlotUnavailableException(String message, List<String> alternativeSuggestions) {
        super(ErrorCode.SLOT_UNAVAILABLE, message, alternativeSuggestions);
    }
}
