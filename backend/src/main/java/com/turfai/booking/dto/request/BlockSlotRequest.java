package com.turfai.booking.dto.request;

import com.turfai.booking.entity.BlockReason;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockSlotRequest {

    @NotNull(message = "Turf ID is required")
    private UUID turfId;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Block date cannot be in the past")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotNull(message = "Reason is required")
    private BlockReason reason;

    @NotNull(message = "Created by user ID is required")
    private UUID createdBy;
}
