package com.turfai.booking.dto.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WebhookValue {

    @JsonProperty("messaging_product")
    private String messagingProduct;

    private WebhookMetadata metadata;

    private List<WebhookContact> contacts;

    private List<WebhookMessage> messages;

    private List<WebhookStatusUpdate> statuses;
}
