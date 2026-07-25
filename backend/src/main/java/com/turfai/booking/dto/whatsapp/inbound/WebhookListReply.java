package com.turfai.booking.dto.whatsapp.inbound;

import lombok.Data;

@Data
public class WebhookListReply {
    private String id;
    private String title;
    private String description;
}
