package com.turfai.booking.dto.whatsapp.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateComponent {
    private String type; // header, body, button
    private List<TemplateParameter> parameters;
}
