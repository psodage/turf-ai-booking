package com.turfai.booking.dto.response;

import com.turfai.booking.entity.BookingSource;
import com.turfai.booking.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private UUID id;
    private String bookingNumber;
    private UUID businessId;
    private UUID turfId;
    private UUID customerId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private BookingStatus status;
    private BookingSource bookingSource;
    private Instant cancelledAt;
    private UUID cancelledBy;
    private Instant createdAt;
}
