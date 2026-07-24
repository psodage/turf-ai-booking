package com.turfai.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TurfAiBookingApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Verifies that Spring application context starts without errors
    }

    @Test
    void actuatorHealthEndpointReturnsOkAndCorrelationId() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void customCorrelationIdHeaderIsPreserved() throws Exception {
        String customCorrelationId = "test-correlation-id-12345";
        mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-ID", customCorrelationId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", customCorrelationId));
    }
}
