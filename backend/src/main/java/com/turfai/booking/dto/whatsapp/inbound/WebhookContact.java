package com.turfai.booking.dto.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookContact {
    private WebhookProfile profile;

    @JsonProperty("wa_id")
    private String waId;
}
