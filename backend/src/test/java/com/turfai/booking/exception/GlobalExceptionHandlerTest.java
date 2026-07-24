package com.turfai.booking.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void handleBaseExceptionReturnsFormattedJson() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/slot-unavailable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SLOT_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.message").value("Slot is already booked."))
                .andExpect(jsonPath("$.error.suggestions[0]").value("18:00"))
                .andExpect(jsonPath("$.error.suggestions[1]").value("20:00"));
    }

    @Test
    void handleValidationExceptionReturns422WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/test-errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.fields[0].field").value("name"))
                .andExpect(jsonPath("$.error.fields[0].message").value("name must not be null"));
    }
}
