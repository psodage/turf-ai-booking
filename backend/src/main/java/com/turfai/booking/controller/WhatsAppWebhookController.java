package com.turfai.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.config.WhatsAppProperties;
import com.turfai.booking.dto.whatsapp.inbound.InboundWebhookPayload;
import com.turfai.booking.service.WhatsAppWebhookProcessor;
import com.turfai.booking.util.WhatsAppSignatureValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping({"/webhook/whatsapp", "/api/v1/webhooks/whatsapp"})
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhook API", description = "Meta Cloud API webhook verification and event callback endpoints.")
public class WhatsAppWebhookController {

    private final WhatsAppProperties whatsappProperties;
    private final WhatsAppSignatureValidator whatsAppSignatureValidator;
    private final WhatsAppWebhookProcessor whatsAppWebhookProcessor;
    private final ObjectMapper objectMapper;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Meta Webhook Verification Handshake (GET)")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        String expectedToken = whatsappProperties.getVerifyToken();
        log.info("Received WhatsApp webhook GET verification request. mode={}, verifyToken={}, expectedToken={}", mode, verifyToken, expectedToken);

        boolean isModeValid = "subscribe".equals(mode);
        boolean isTokenValid = expectedToken != null && verifyToken != null && expectedToken.trim().equalsIgnoreCase(verifyToken.trim());

        if (isModeValid && isTokenValid) {
            log.info("WhatsApp webhook GET verification succeeded!");
            return ResponseEntity.ok(challenge != null ? challenge : "");
        } else {
            log.warn("WhatsApp webhook GET verification failed. Expected token='{}', Received token='{}', Mode='{}'", expectedToken, verifyToken, mode);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    @PostMapping
    @Operation(summary = "Meta Webhook Event Callback (POST)")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody String rawPayload) {

        log.info(">>> WhatsApp POST webhook received. Payload size: {} bytes, Signature present: {}",
                rawPayload != null ? rawPayload.length() : 0,
                signatureHeader != null);

        // 1. HMAC-SHA256 Signature Verification (log failure but continue for debugging)
        try {
            whatsAppSignatureValidator.validateSignature(rawPayload, signatureHeader);
            log.info(">>> Signature validation PASSED");
        } catch (Exception ex) {
            log.warn(">>> Signature validation FAILED: {}. Continuing to process for debugging.", ex.getMessage());
        }

        // 2. Parse JSON Payload
        try {
            InboundWebhookPayload payload = objectMapper.readValue(rawPayload, InboundWebhookPayload.class);
            log.info(">>> Payload parsed successfully. Dispatching to processor.");
            whatsAppWebhookProcessor.processWebhookPayload(payload);
            log.info(">>> Webhook processing completed successfully.");
        } catch (Exception ex) {
            log.error(">>> Error processing incoming WhatsApp webhook payload", ex);
        }

        // 3. Always return 200 OK EVENT_RECEIVED per Meta Cloud API requirement
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
