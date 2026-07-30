package com.turfai.booking.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String provider = "mock"; // mock | openai | gemini
    private String apiKey = "demo_ai_api_key";
    private String model = "gpt-4o-mini";
    private String apiUrl = "https://api.openai.com/v1/chat/completions";
    private int maxTokens = 2000;
    private int maxContextMessages = 10;
    private int sessionTimeoutMinutes = 10;
    private int maxTurnsPerSession = 20;
}
