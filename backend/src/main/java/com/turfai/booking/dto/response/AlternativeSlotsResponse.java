package com.turfai.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlternativeSlotsResponse {

    private UUID turfId;
    private LocalDate date;
    private List<SlotResponse> suggestedSlots;
}
