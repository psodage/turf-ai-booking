package com.turfai.booking.integration;

import com.turfai.booking.config.CorrelationIdFilter;
import com.turfai.booking.config.RateLimitingFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityMonitoringIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RateLimitingFilter rateLimitingFilter;

    @Test
    @DisplayName("1. Actuator Health Endpoint: Should return 200 OK and UP status")
    void testActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("2. Actuator Metrics Endpoint: Should expose metrics endpoint")
    void testActuatorMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("3. Security Headers: Should attach OWASP security headers to responses")
    void testSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'self'"));
    }

    @Test
    @DisplayName("4. Correlation ID Propagation: Should echo or generate X-Correlation-ID header")
    void testCorrelationIdHeaderPropagation() throws Exception {
        String testCorrelationId = "TEST-CORRELATION-ID-999";

        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, testCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, testCorrelationId));
    }

    @Test
    @DisplayName("5. Rate Limiting Throttling: Exceeding request threshold returns 429 Too Many Requests")
    void testRateLimitingThrottling() throws Exception {
        String testIp = "192.168.99.100";

        // Perform requests up to limit
        for (int i = 0; i < RateLimitingFilter.WEBHOOK_RATE_LIMIT; i++) {
            mockMvc.perform(get("/webhook/whatsapp")
                            .header("X-Forwarded-For", testIp)
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "invalid")
                            .param("hub.challenge", "123"))
                    .andExpect(status().isForbidden());
        }

        // 101st request should be throttled
        mockMvc.perform(get("/webhook/whatsapp")
                        .header("X-Forwarded-For", testIp)
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "invalid")
                        .param("hub.challenge", "123"))
                .andExpect(status().isTooManyRequests());
    }
}
