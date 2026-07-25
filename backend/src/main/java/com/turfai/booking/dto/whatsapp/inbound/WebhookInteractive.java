package com.turfai.booking.dto.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookInteractive {
    private String type; // button_reply, list_reply

    @JsonProperty("button_reply")
    private WebhookButtonReply buttonReply;

    @JsonProperty("list_reply")
    private WebhookListReply listReply;
}
