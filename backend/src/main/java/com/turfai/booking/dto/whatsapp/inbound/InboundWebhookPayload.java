package com.turfai.booking.dto.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InboundWebhookPayload {
    private String object;
    private List<WebhookEntry> entry;
}
