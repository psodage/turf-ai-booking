package com.turfai.booking.util;

import com.turfai.booking.config.WhatsAppProperties;
import com.turfai.booking.exception.WebhookSignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppSignatureValidator {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private final WhatsAppProperties whatsappProperties;

    public void validateSignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.warn("Missing or malformed X-Hub-Signature-256 header");
            throw new WebhookSignatureException("Missing or malformed X-Hub-Signature-256 header");
        }

        String expectedSignature = signatureHeader.substring(7);
        String calculatedSignature = calculateHmacSha256(rawPayload, whatsappProperties.getAppSecret());

        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), calculatedSignature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Signature mismatch. Calculated: {}, Provided: {}", calculatedSignature, expectedSignature);
            throw new WebhookSignatureException("Invalid webhook signature");
        }
    }

    public String calculateHmacSha256(String data, String secret) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            log.error("Failed to compute HMAC-SHA256 signature", ex);
            throw new WebhookSignatureException("Failed to calculate HMAC signature");
        }
    }
}
