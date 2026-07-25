package com.turfai.booking.dto.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookMessage {

    private String id; // Meta wamid
    private String from;
    private String timestamp;
    private String type; // text, interactive, location, etc.
    private WebhookText text;
    private WebhookInteractive interactive;
}
