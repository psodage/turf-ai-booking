package com.turfai.booking.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class RazorpayWebhookPayload {

    private String event;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("created_at")
    private Long createdAt;

    private Map<String, Object> payload;
}
