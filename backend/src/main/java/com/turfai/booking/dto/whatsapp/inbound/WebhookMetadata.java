package com.turfai.booking.dto.whatsapp.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WebhookMetadata {

    @JsonProperty("display_phone_number")
    private String displayPhoneNumber;

    @JsonProperty("phone_number_id")
    private String phoneNumberId;
}
