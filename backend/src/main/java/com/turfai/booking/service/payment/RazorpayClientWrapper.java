package com.turfai.booking.service.payment;

import java.math.BigDecimal;

public interface RazorpayClientWrapper {
    PaymentLinkDto createPaymentLink(BigDecimal amount, String description, String customerName, String customerPhone, String bookingNumber);
    RefundResultDto initiateRefund(String paymentId, BigDecimal amount, String reason);
}
