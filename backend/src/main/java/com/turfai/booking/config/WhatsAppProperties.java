package com.turfai.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    private String apiUrl = "https://graph.facebook.com/v20.0";
    private String phoneNumberId;
    private String businessAccountId;
    private String accessToken;
    private String verifyToken = "turfai_verify_token_dev";
    private String appSecret = "turfai_app_secret_dev";
}
