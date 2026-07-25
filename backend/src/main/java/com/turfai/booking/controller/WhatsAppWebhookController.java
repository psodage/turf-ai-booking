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
@RequestMapping("/webhook/whatsapp")
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

        log.info("Received WhatsApp webhook GET verification request. mode={}", mode);

        if ("subscribe".equals(mode) && whatsappProperties.getVerifyToken().equals(verifyToken)) {
            log.info("WhatsApp webhook GET verification succeeded!");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("WhatsApp webhook GET verification failed. Token mismatch or invalid mode.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Meta Webhook Event Callback (POST)")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestBody String rawPayload) {

        log.debug("Received WhatsApp webhook POST event. Payload size: {} bytes", rawPayload != null ? rawPayload.length() : 0);

        // 1. HMAC-SHA256 Signature Verification
        whatsAppSignatureValidator.validateSignature(rawPayload, signatureHeader);

        // 2. Parse JSON Payload
        try {
            InboundWebhookPayload payload = objectMapper.readValue(rawPayload, InboundWebhookPayload.class);
            whatsAppWebhookProcessor.processWebhookPayload(payload);
        } catch (Exception ex) {
            log.error("Error processing incoming WhatsApp webhook payload", ex);
        }

        // 3. Always return 200 OK EVENT_RECEIVED per Meta Cloud API requirement
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
