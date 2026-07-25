package com.turfai.booking.dto.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookStatusUpdate {

    private String id;
    private String status; // sent, delivered, read, failed
    private String timestamp;

    @JsonProperty("recipient_id")
    private String recipientId;
}
