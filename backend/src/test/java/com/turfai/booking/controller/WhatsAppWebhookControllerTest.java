package com.turfai.booking.controller;

import com.turfai.booking.config.WhatsAppProperties;
import com.turfai.booking.service.WhatsAppWebhookProcessor;
import com.turfai.booking.util.WhatsAppSignatureValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WhatsAppWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class WhatsAppWebhookControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private WhatsAppProperties whatsappProperties;
    @MockBean private WhatsAppSignatureValidator whatsAppSignatureValidator;
    @MockBean private WhatsAppWebhookProcessor whatsAppWebhookProcessor;

    @Test
    @DisplayName("GET /webhook/whatsapp should return 200 OK with challenge when verify_token matches")
    void testWebhookVerificationSuccess() throws Exception {
        when(whatsappProperties.getVerifyToken()).thenReturn("turfai_secret_token");

        mockMvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "turfai_secret_token")
                        .param("hub.challenge", "CHALLENGE_12345"))
                .andExpect(status().isOk())
                .andExpect(content().string("CHALLENGE_12345"));
    }

    @Test
    @DisplayName("GET /webhook/whatsapp should return 403 FORBIDDEN when verify_token does not match")
    void testWebhookVerificationFailure() throws Exception {
        when(whatsappProperties.getVerifyToken()).thenReturn("turfai_secret_token");

        mockMvc.perform(get("/webhook/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong_token")
                        .param("hub.challenge", "CHALLENGE_12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /webhook/whatsapp should validate signature and process payload")
    void testReceiveWebhookSuccess() throws Exception {
        String payload = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";

        doNothing().when(whatsAppSignatureValidator).validateSignature(anyString(), anyString());
        doNothing().when(whatsAppWebhookProcessor).processWebhookPayload(any());

        mockMvc.perform(post("/webhook/whatsapp")
                        .header("X-Hub-Signature-256", "sha256=mocked_signature")
                        .content(payload)
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string("EVENT_RECEIVED"));
    }
}
