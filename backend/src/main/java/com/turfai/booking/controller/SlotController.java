package com.turfai.booking.controller;

import com.turfai.booking.dto.response.AlternativeSlotsResponse;
import com.turfai.booking.dto.response.DaySlotsResponse;
import com.turfai.booking.service.BookingService;
import com.turfai.booking.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/turfs")
@RequiredArgsConstructor
@Tag(name = "Turf Slots API", description = "Endpoints for checking slot availability and alternative slot suggestions.")
public class SlotController {

    private final SlotService slotService;
    private final BookingService bookingService;

    @GetMapping("/{turfId}/slots")
    @Operation(summary = "Get available 60-minute time slots for a turf on a specific date")
    public ResponseEntity<DaySlotsResponse> getSlots(
            @PathVariable UUID turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DaySlotsResponse response = slotService.getAvailableSlots(turfId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{turfId}/slots/alternatives")
    @Operation(summary = "Get up to 3 suggested alternative available slots for a turf on a date")
    public ResponseEntity<AlternativeSlotsResponse> getAlternativeSlots(
            @PathVariable UUID turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AlternativeSlotsResponse response = bookingService.suggestAlternativeSlots(turfId, date);
        return ResponseEntity.ok(response);
    }
}
