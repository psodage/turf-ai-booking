package com.turfai.booking.exception;

/**
 * Thrown when requested booking slot falls outside configured turf operating hours.
 */
public class OutsideOperatingHoursException extends BaseException {

    public OutsideOperatingHoursException() {
        super(ErrorCode.OUTSIDE_OPERATING_HOURS);
    }

    public OutsideOperatingHoursException(String message) {
        super(ErrorCode.OUTSIDE_OPERATING_HOURS, message);
    }
}
