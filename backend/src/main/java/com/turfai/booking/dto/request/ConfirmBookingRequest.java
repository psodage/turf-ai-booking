package com.turfai.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmBookingRequest {

    @NotBlank(message = "Gateway payment ID is required")
    private String gatewayPaymentId;
}
