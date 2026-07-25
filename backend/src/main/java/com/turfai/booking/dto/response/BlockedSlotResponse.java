package com.turfai.booking.dto.response;

import com.turfai.booking.entity.BlockReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedSlotResponse {

    private UUID id;
    private UUID turfId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private BlockReason reason;
    private UUID createdBy;
    private Instant createdAt;
}
