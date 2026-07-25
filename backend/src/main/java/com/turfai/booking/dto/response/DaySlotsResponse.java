package com.turfai.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DaySlotsResponse {

    private UUID turfId;
    private LocalDate date;
    private int dayOfWeek;
    private boolean isClosed;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private List<SlotResponse> slots;
}
