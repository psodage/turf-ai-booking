package com.turfai.booking.dto.payment;

import com.turfai.booking.entity.PaymentStatus;
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
public class PaymentResponse {

    private UUID paymentId;
    private UUID bookingId;
    private String bookingNumber;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String gatewayPaymentId;
    private String paymentUrl;
    private Instant createdAt;
}
