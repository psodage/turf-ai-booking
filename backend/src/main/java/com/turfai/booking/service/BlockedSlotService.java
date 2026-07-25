package com.turfai.booking.service;

import com.turfai.booking.dto.request.BlockSlotRequest;
import com.turfai.booking.dto.response.BlockedSlotResponse;
import com.turfai.booking.entity.BlockedSlot;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.User;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.exception.ErrorCode;
import com.turfai.booking.exception.SlotUnavailableException;
import com.turfai.booking.repository.BlockedSlotRepository;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockedSlotService {

    private final BlockedSlotRepository blockedSlotRepository;
    private final TurfService turfService;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final SlotService slotService;

    @Transactional
    public BlockedSlotResponse blockSlot(BlockSlotRequest request) {
        Turf turf = turfService.getTurfById(request.getTurfId());
        User creator = userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + request.getCreatedBy()) {});

        // Validate no active booking exists for the slot
        List<Booking> activeBookings = bookingRepository.findByTurfIdAndBookingDateAndStatusIn(
                turf.getId(), request.getDate(), Set.of(BookingStatus.HOLD, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED));

        boolean hasActiveBooking = activeBookings.stream()
                .anyMatch(b -> slotService.timesOverlap(request.getStartTime(), request.getEndTime(), b.getStartTime(), b.getEndTime()));

        if (hasActiveBooking) {
            throw new SlotUnavailableException("Cannot block slot: an active booking exists for this period.");
        }

        BlockedSlot blockedSlot = BlockedSlot.builder()
                .turf(turf)
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .createdBy(creator)
                .build();

        blockedSlot = blockedSlotRepository.save(blockedSlot);
        return mapToBlockedSlotResponse(blockedSlot);
    }

    @Transactional
    public void unblockSlot(UUID blockedSlotId) {
        BlockedSlot blockedSlot = blockedSlotRepository.findById(blockedSlotId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Blocked slot not found with ID: " + blockedSlotId) {});
        blockedSlotRepository.delete(blockedSlot);
    }

    @Transactional(readOnly = true)
    public List<BlockedSlotResponse> getBlockedSlots(UUID turfId, LocalDate date) {
        return blockedSlotRepository.findByTurfIdAndDate(turfId, date).stream()
                .map(this::mapToBlockedSlotResponse)
                .toList();
    }

    private BlockedSlotResponse mapToBlockedSlotResponse(BlockedSlot b) {
        return BlockedSlotResponse.builder()
                .id(b.getId())
                .turfId(b.getTurf().getId())
                .date(b.getDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .reason(b.getReason())
                .createdBy(b.getCreatedBy().getId())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
