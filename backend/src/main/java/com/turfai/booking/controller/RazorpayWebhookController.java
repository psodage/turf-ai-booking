package com.turfai.booking.controller;

import com.turfai.booking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping({"/webhook/razorpay", "/api/v1/payments/webhook"})
@RequiredArgsConstructor
@Tag(name = "Razorpay Webhook API", description = "Authoritative payment completion webhook callback receiver endpoint.")
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Razorpay Payment Webhook Callback (POST)")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signatureHeader,
            @RequestBody String rawPayload) {

        log.info(">>> Razorpay POST webhook received. Payload size: {} bytes, Signature present: {}",
                rawPayload != null ? rawPayload.length() : 0,
                signatureHeader != null);

        try {
            paymentService.processRazorpayWebhook(rawPayload, signatureHeader);
            log.info(">>> Razorpay webhook processed successfully.");
        } catch (Exception ex) {
            log.error(">>> Error processing Razorpay webhook payload", ex);
        }

        return ResponseEntity.ok("OK");
    }
}
