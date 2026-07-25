package com.turfai.booking.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentLinkRequest {

    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    private String callbackUrl;
}
