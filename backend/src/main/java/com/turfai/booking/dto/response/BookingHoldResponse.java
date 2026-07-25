package com.turfai.booking.dto.response;

import com.turfai.booking.entity.HoldStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHoldResponse {

    private UUID holdId;
    private UUID bookingId;
    private String bookingNumber;
    private BigDecimal price;
    private Instant expiresAt;
    private HoldStatus status;
}
