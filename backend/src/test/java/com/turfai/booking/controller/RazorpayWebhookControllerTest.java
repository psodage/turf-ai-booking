package com.turfai.booking.controller;

import com.turfai.booking.service.PaymentService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RazorpayWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RazorpayWebhookControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private PaymentService paymentService;

    @Test
    @DisplayName("POST /webhook/razorpay should process payload and return 200 OK")
    void testReceiveWebhookSuccess() throws Exception {
        String payload = "{\"event\":\"payment.link.paid\",\"payload\":{}}";

        doNothing().when(paymentService).processRazorpayWebhook(anyString(), anyString());

        mockMvc.perform(post("/webhook/razorpay")
                        .header("X-Razorpay-Signature", "mocked_signature")
                        .content(payload)
                        .contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
