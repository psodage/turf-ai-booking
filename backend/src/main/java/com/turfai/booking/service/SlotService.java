package com.turfai.booking.service;

import com.turfai.booking.dto.response.DaySlotsResponse;
import com.turfai.booking.dto.response.SlotResponse;
import com.turfai.booking.entity.BlockedSlot;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingHold;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.HoldStatus;
import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.repository.BlockedSlotRepository;
import com.turfai.booking.repository.BookingHoldRepository;
import com.turfai.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotService {

    public static final int SLOT_DURATION_MINUTES = 60;
    public static final int ADVANCE_BOOKING_DAYS_DEFAULT = 30;

    private final TurfService turfService;
    private final OperatingHoursService operatingHoursService;
    private final PricingService pricingService;
    private final BlockedSlotRepository blockedSlotRepository;
    private final BookingRepository bookingRepository;
    private final BookingHoldRepository bookingHoldRepository;

    public DaySlotsResponse getAvailableSlots(UUID turfId, LocalDate date) {
        Turf turf = turfService.getTurfById(turfId);
        Business business = turf.getBusiness();
        ZoneId businessZone = ZoneId.of(business.getTimezone());
        ZonedDateTime nowInBusiness = ZonedDateTime.now(businessZone);
        LocalDate todayInBusiness = nowInBusiness.toLocalDate();
        LocalTime currentTimeInBusiness = nowInBusiness.toLocalTime();

        Optional<OperatingHours> opHoursOpt = operatingHoursService.getOperatingHours(turfId, date);
        if (opHoursOpt.isEmpty() || Boolean.TRUE.equals(opHoursOpt.get().getIsClosed())) {
            return DaySlotsResponse.builder()
                    .turfId(turfId)
                    .date(date)
                    .dayOfWeek(operatingHoursService.toDayOfWeekIndex(date))
                    .isClosed(true)
                    .slots(List.of())
                    .build();
        }

        OperatingHours opHours = opHoursOpt.get();
        LocalTime openTime = opHours.getOpeningTime();
        LocalTime closeTime = opHours.getClosingTime();

        // Check valid operating hours
        if (openTime == null || closeTime == null || !openTime.isBefore(closeTime)) {
            return DaySlotsResponse.builder()
                    .turfId(turfId)
                    .date(date)
                    .dayOfWeek(operatingHoursService.toDayOfWeekIndex(date))
                    .isClosed(false)
                    .openingTime(openTime)
                    .closingTime(closeTime)
                    .slots(List.of())
                    .build();
        }

        // Validate 30-day advance booking window and past dates
        if (date.isBefore(todayInBusiness) || date.isAfter(todayInBusiness.plusDays(ADVANCE_BOOKING_DAYS_DEFAULT))) {
            return DaySlotsResponse.builder()
                    .turfId(turfId)
                    .date(date)
                    .dayOfWeek(operatingHoursService.toDayOfWeekIndex(date))
                    .isClosed(false)
                    .openingTime(openTime)
                    .closingTime(closeTime)
                    .slots(List.of())
                    .build();
        }

        // Fetch blocked slots and active bookings
        List<BlockedSlot> blockedSlots = blockedSlotRepository.findByTurfIdAndDate(turfId, date);
        List<Booking> activeBookings = bookingRepository.findByTurfIdAndBookingDateAndStatusIn(
                turfId, date, Set.of(BookingStatus.HOLD, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED));

        // Filter out lazy expired holds
        Instant nowUtc = Instant.now();
        List<Booking> validActiveBookings = activeBookings.stream()
                .filter(b -> isBookingActiveAndUnexpired(b, nowUtc))
                .toList();

        List<SlotResponse> slotResponses = new ArrayList<>();
        long totalOperatingMinutes = java.time.Duration.between(openTime, closeTime).toMinutes();
        long totalSlots = totalOperatingMinutes / SLOT_DURATION_MINUTES;

        for (int i = 0; i < totalSlots; i++) {
            LocalTime slotStart = openTime.plusMinutes((long) i * SLOT_DURATION_MINUTES);
            LocalTime slotEnd = slotStart.plusMinutes(SLOT_DURATION_MINUTES);

            boolean isPast = date.equals(todayInBusiness) && slotStart.isBefore(currentTimeInBusiness);
            boolean isBlocked = isSlotBlocked(slotStart, slotEnd, blockedSlots);
            boolean isBooked = isSlotBooked(slotStart, slotEnd, validActiveBookings);

            boolean available = !isPast && !isBlocked && !isBooked;
            String unavailableReason = null;
            if (isPast) {
                unavailableReason = "Time slot has passed";
            } else if (isBlocked) {
                unavailableReason = "Blocked by owner";
            } else if (isBooked) {
                unavailableReason = "Slot already booked";
            }

            PricingService.PriceResult priceResult = pricingService.calculateSlotPrice(turfId, date, slotStart, slotEnd);

            slotResponses.add(SlotResponse.builder()
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .available(available)
                    .price(priceResult.amount())
                    .pricingType(priceResult.type())
                    .unavailableReason(unavailableReason)
                    .build());
        }

        return DaySlotsResponse.builder()
                .turfId(turfId)
                .date(date)
                .dayOfWeek(operatingHoursService.toDayOfWeekIndex(date))
                .isClosed(false)
                .openingTime(openTime)
                .closingTime(closeTime)
                .slots(slotResponses)
                .build();
    }

    private boolean isBookingActiveAndUnexpired(Booking booking, Instant nowUtc) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return true;
        }
        Optional<BookingHold> holdOpt = bookingHoldRepository.findByBookingId(booking.getId());
        if (holdOpt.isEmpty()) {
            return false;
        }
        BookingHold hold = holdOpt.get();
        return hold.getStatus() == HoldStatus.ACTIVE && hold.getExpiresAt().isAfter(nowUtc);
    }

    private boolean isSlotBlocked(LocalTime start, LocalTime end, List<BlockedSlot> blockedSlots) {
        return blockedSlots.stream().anyMatch(b -> timesOverlap(start, end, b.getStartTime(), b.getEndTime()));
    }

    private boolean isSlotBooked(LocalTime start, LocalTime end, List<Booking> bookings) {
        return bookings.stream().anyMatch(b -> timesOverlap(start, end, b.getStartTime(), b.getEndTime()));
    }

    public boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
