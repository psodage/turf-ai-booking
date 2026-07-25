package com.turfai.booking.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "razorpay.mode", havingValue = "mock", matchIfMissing = true)
public class MockRazorpayClientWrapper implements RazorpayClientWrapper {

    @Override
    public PaymentLinkDto createPaymentLink(BigDecimal amount, String description, String customerName, String customerPhone, String bookingNumber) {
        String mockLinkId = "plink_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String mockUrl = "https://rzp.io/i/" + mockLinkId;

        log.info("Mock Razorpay Payment Link created: id={}, amount={}, url={}", mockLinkId, amount, mockUrl);

        return PaymentLinkDto.builder()
                .linkId(mockLinkId)
                .shortUrl(mockUrl)
                .status("created")
                .amount(amount)
                .build();
    }

    @Override
    public RefundResultDto initiateRefund(String paymentId, BigDecimal amount, String reason) {
        String mockRefundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        log.info("Mock Razorpay Refund initiated: refundId={}, paymentId={}, amount={}, reason={}",
                mockRefundId, paymentId, amount, reason);

        return RefundResultDto.builder()
                .refundId(mockRefundId)
                .paymentId(paymentId)
                .amount(amount)
                .status("processed")
                .build();
    }
}
