package com.turfai.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    private String mode = "mock"; // mock | test | live
    private String keyId = "rzp_test_demo_key_id";
    private String keySecret = "rzp_test_demo_key_secret";
    private String webhookSecret = "turfai_rzp_webhook_secret_dev";
}
