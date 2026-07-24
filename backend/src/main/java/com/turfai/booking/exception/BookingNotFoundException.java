package com.turfai.booking.exception;

/**
 * Thrown when a booking cannot be located by ID or booking reference number.
 */
public class BookingNotFoundException extends BaseException {

    public BookingNotFoundException() {
        super(ErrorCode.BOOKING_NOT_FOUND);
    }

    public BookingNotFoundException(String message) {
        super(ErrorCode.BOOKING_NOT_FOUND, message);
    }
}
