package com.turfai.booking.controller;

import com.turfai.booking.dto.request.BlockSlotRequest;
import com.turfai.booking.dto.response.BlockedSlotResponse;
import com.turfai.booking.service.BlockedSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/turfs")
@RequiredArgsConstructor
@Tag(name = "Blocked Slots API", description = "Endpoints for turf owners/managers to block and unblock slots.")
public class BlockedSlotController {

    private final BlockedSlotService blockedSlotService;

    @PostMapping("/{turfId}/blocked-slots")
    @Operation(summary = "Block a time slot (owner/manager operation)")
    public ResponseEntity<BlockedSlotResponse> blockSlot(
            @PathVariable UUID turfId,
            @Valid @RequestBody BlockSlotRequest request) {
        request.setTurfId(turfId);
        BlockedSlotResponse response = blockedSlotService.blockSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{turfId}/blocked-slots/{id}")
    @Operation(summary = "Unblock a previously blocked time slot")
    public ResponseEntity<Void> unblockSlot(@PathVariable UUID turfId, @PathVariable UUID id) {
        blockedSlotService.unblockSlot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{turfId}/blocked-slots")
    @Operation(summary = "List all blocked slots for a turf on a specific date")
    public ResponseEntity<List<BlockedSlotResponse>> getBlockedSlots(
            @PathVariable UUID turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<BlockedSlotResponse> response = blockedSlotService.getBlockedSlots(turfId, date);
        return ResponseEntity.ok(response);
    }
}
