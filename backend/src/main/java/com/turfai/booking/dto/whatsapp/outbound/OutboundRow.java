package com.turfai.booking.dto.whatsapp.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundRow {
    private String id;
    private String title;
    private String description;
}
