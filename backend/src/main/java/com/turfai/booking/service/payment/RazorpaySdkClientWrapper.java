package com.turfai.booking.service.payment;

import com.turfai.booking.config.RazorpayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("'${razorpay.mode:mock}'.equalsIgnoreCase('live') || '${razorpay.mode:mock}'.equalsIgnoreCase('test')")
@RequiredArgsConstructor
public class RazorpaySdkClientWrapper implements RazorpayClientWrapper {

    private final RazorpayProperties razorpayProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @SuppressWarnings("unchecked")
    public PaymentLinkDto createPaymentLink(BigDecimal amount, String description, String customerName, String customerPhone, String bookingNumber) {
        String url = "https://api.razorpay.com/v1/payment_links";

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue()); // Amount in paise
        body.put("currency", "INR");
        body.put("accept_partial", false);
        body.put("description", description);

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", customerName);
        customer.put("contact", customerPhone);
        body.put("customer", customer);

        Map<String, String> notes = new HashMap<>();
        notes.put("booking_number", bookingNumber);
        body.put("notes", notes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> res = restTemplate.postForObject(url, requestEntity, Map.class);
            return PaymentLinkDto.builder()
                    .linkId((String) res.get("id"))
                    .shortUrl((String) res.get("short_url"))
                    .status((String) res.get("status"))
                    .amount(amount)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to create Razorpay Payment Link", ex);
            throw new RuntimeException("Failed to generate payment link via Razorpay: " + ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public RefundResultDto initiateRefund(String paymentId, BigDecimal amount, String reason) {
        String url = String.format("https://api.razorpay.com/v1/payments/%s/refund", paymentId);

        Map<String, Object> body = new HashMap<>();
        if (amount != null) {
            body.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue());
        }
        body.put("notes", Map.of("reason", reason != null ? reason : "Requested by system"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> res = restTemplate.postForObject(url, requestEntity, Map.class);
            return RefundResultDto.builder()
                    .refundId((String) res.get("id"))
                    .paymentId((String) res.get("payment_id"))
                    .amount(amount)
                    .status((String) res.get("status"))
                    .build();
        } catch (Exception ex) {
            log.error("Failed to initiate Razorpay refund for payment {}", paymentId, ex);
            throw new RuntimeException("Failed to process refund via Razorpay: " + ex.getMessage(), ex);
        }
    }
}
