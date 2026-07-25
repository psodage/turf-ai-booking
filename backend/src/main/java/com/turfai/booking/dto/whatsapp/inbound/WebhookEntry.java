package com.turfai.booking.dto.whatsapp.inbound;

import lombok.Data;

import java.util.List;

@Data
public class WebhookEntry {
    private String id;
    private List<WebhookChange> changes;
}
