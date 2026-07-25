package com.turfai.booking.service.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkDto {
    private String linkId;
    private String shortUrl;
    private String status;
    private BigDecimal amount;
}
