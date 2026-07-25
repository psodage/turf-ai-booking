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
public class OutboundSection {
    private String title;
    private List<OutboundRow> rows;
}
