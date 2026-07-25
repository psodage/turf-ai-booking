package com.turfai.booking.dto.whatsapp.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateParameter {
    private String type; // text, currency, date_time
    private String text;
}
