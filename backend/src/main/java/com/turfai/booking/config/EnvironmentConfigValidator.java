package com.turfai.booking.config;

import com.turfai.booking.ai.config.AiProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Startup validator for environment variables and secrets configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentConfigValidator {

    private final Environment environment;
    private final WhatsAppProperties whatsAppProperties;
    private final RazorpayProperties razorpayProperties;
    private final AiProperties aiProperties;

    @PostConstruct
    public void validateEnvironment() {
        boolean isProdOrStaging = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "staging".equalsIgnoreCase(p));

        log.info("Active environment profiles: {}", Arrays.toString(environment.getActiveProfiles()));

        if (isProdOrStaging) {
            if ("dev_app_secret".equalsIgnoreCase(whatsAppProperties.getAppSecret())
                    || whatsAppProperties.getAppSecret() == null || whatsAppProperties.getAppSecret().isBlank()) {
                log.error("CRITICAL SECURITY WARNING: WhatsApp App Secret is set to default/dev secret in non-dev profile!");
            }

            if ("dev_webhook_secret".equalsIgnoreCase(razorpayProperties.getWebhookSecret())
                    || razorpayProperties.getWebhookSecret() == null || razorpayProperties.getWebhookSecret().isBlank()) {
                log.error("CRITICAL SECURITY WARNING: Razorpay Webhook Secret is set to default/dev secret in non-dev profile!");
            }

            if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
                log.error("CRITICAL SECURITY WARNING: AI API key is missing!");
            }
        }
    }
}
