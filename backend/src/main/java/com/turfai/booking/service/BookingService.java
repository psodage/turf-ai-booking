package com.turfai.booking.service;

import com.turfai.booking.dto.request.CancelBookingRequest;
import com.turfai.booking.dto.request.CreateBookingHoldRequest;
import com.turfai.booking.dto.response.AlternativeSlotsResponse;
import com.turfai.booking.dto.response.BookingHoldResponse;
import com.turfai.booking.dto.response.BookingResponse;
import com.turfai.booking.dto.response.DaySlotsResponse;
import com.turfai.booking.dto.response.SlotResponse;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingAudit;
import com.turfai.booking.entity.BookingHold;
import com.turfai.booking.entity.BookingSource;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.HoldStatus;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.exception.BookingNotFoundException;
import com.turfai.booking.exception.CancellationDeniedException;
import com.turfai.booking.exception.ErrorCode;
import com.turfai.booking.exception.HoldExpiredException;
import com.turfai.booking.exception.OutsideOperatingHoursException;
import com.turfai.booking.exception.SlotUnavailableException;
import com.turfai.booking.repository.BookingAuditRepository;
import com.turfai.booking.repository.BookingHoldRepository;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    public static final double HOLD_DURATION_MINUTES = 7.5;
    public static final int CANCELLATION_WINDOW_HOURS = 2;
    public static final int PAYMENT_GRACE_PERIOD_SECONDS = 60;

    private final TurfService turfService;
    private final OperatingHoursService operatingHoursService;
    private final PricingService pricingService;
    private final SlotService slotService;
    private final BookingRepository bookingRepository;
    private final BookingHoldRepository bookingHoldRepository;
    private final BookingAuditRepository bookingAuditRepository;
    private final UserRepository userRepository;
    private final WhatsAppService whatsAppService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates a temporary booking hold (10 minutes, ADR-005, ADR-014).
     */
    @Transactional
    public BookingHoldResponse createBookingHold(CreateBookingHoldRequest request) {
        Turf turf = turfService.getTurfById(request.getTurfId());
        Business business = turf.getBusiness();
        ZoneId businessZone = ZoneId.of(business.getTimezone());
        ZonedDateTime nowInBusiness = ZonedDateTime.now(businessZone);

        // 1. Validate date & time in business timezone
        LocalDate todayInBusiness = nowInBusiness.toLocalDate();
        LocalTime currentTimeInBusiness = nowInBusiness.toLocalTime();

        if (request.getBookingDate().isBefore(todayInBusiness)) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "Booking date cannot be in the past.") {};
        }

        if (request.getBookingDate().isAfter(todayInBusiness.plusDays(SlotService.ADVANCE_BOOKING_DAYS_DEFAULT))) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "Bookings can only be made up to " + SlotService.ADVANCE_BOOKING_DAYS_DEFAULT + " days in advance.") {};
        }

        if (request.getBookingDate().equals(todayInBusiness) && request.getStartTime().isBefore(currentTimeInBusiness)) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "Cannot book a time slot that has already passed.") {};
        }

        // 2. Validate operating hours
        if (!operatingHoursService.isWithinOperatingHours(turf.getId(), request.getBookingDate(), request.getStartTime(), request.getEndTime())) {
            throw new OutsideOperatingHoursException("Requested time slot is outside operating hours.");
        }

        // Check for existing active bookings / holds
        Instant nowUtcCheck = Instant.now();
        List<Booking> activeBookings = bookingRepository.findByTurfIdAndBookingDateAndStatusIn(
                turf.getId(), request.getBookingDate(), Set.of(BookingStatus.HOLD, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED));

        List<Booking> trulyActiveBookings = activeBookings.stream()
                .filter(b -> {
                    if (b.getStatus() == BookingStatus.CONFIRMED) {
                        return true;
                    }
                    Optional<BookingHold> holdOpt = bookingHoldRepository.findByBookingId(b.getId());
                    if (holdOpt.isPresent()) {
                        BookingHold h = holdOpt.get();
                        if (h.getStatus() == HoldStatus.ACTIVE && h.getExpiresAt().isBefore(nowUtcCheck)) {
                            h.setStatus(HoldStatus.EXPIRED);
                            bookingHoldRepository.save(h);
                            b.setStatus(BookingStatus.EXPIRED);
                            bookingRepository.save(b);
                            return false;
                        }
                        return h.getStatus() == HoldStatus.ACTIVE;
                    }
                    return true;
                })
                .toList();

        // Re-use active hold if customer asks for the exact same slot again
        Optional<Booking> sameCustomerHold = trulyActiveBookings.stream()
                .filter(b -> b.getCustomer().getId().equals(request.getCustomerId())
                        && (b.getStatus() == BookingStatus.HOLD || b.getStatus() == BookingStatus.PAYMENT_PENDING)
                        && b.getStartTime().equals(request.getStartTime())
                        && b.getEndTime().equals(request.getEndTime()))
                .findFirst();

        if (sameCustomerHold.isPresent()) {
            Booking existing = sameCustomerHold.get();
            Optional<BookingHold> hOpt = bookingHoldRepository.findByBookingId(existing.getId());
            if (hOpt.isPresent() && hOpt.get().getStatus() == HoldStatus.ACTIVE) {
                BookingHold existingHold = hOpt.get();
                log.info("Returning existing active booking hold for customer {} and booking {}", request.getCustomerId(), existing.getBookingNumber());
                return BookingHoldResponse.builder()
                        .holdId(existingHold.getId())
                        .bookingId(existing.getId())
                        .bookingNumber(existing.getBookingNumber())
                        .price(existing.getPrice())
                        .expiresAt(existingHold.getExpiresAt())
                        .status(existingHold.getStatus())
                        .build();
            }
        }

        boolean isAlreadyBooked = trulyActiveBookings.stream()
                .anyMatch(b -> slotService.timesOverlap(request.getStartTime(), request.getEndTime(), b.getStartTime(), b.getEndTime()));
        if (isAlreadyBooked) {
            throw new SlotUnavailableException("Requested time slot is already booked or on hold.");
        }

        // 3. Validate user/customer
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Customer not found with ID: " + request.getCustomerId()) {});

        // 4. Lock price
        PricingService.PriceResult priceResult = pricingService.calculateSlotPrice(turf.getId(), request.getBookingDate(), request.getStartTime(), request.getEndTime());

        // 5. Generate booking number BK-YYYY-NNNNN
        String bookingNumber = generateBookingNumber(request.getBookingDate().getYear());

        // 6. Build & Save Booking & BookingHold
        Instant nowUtc = Instant.now();
        Instant expiresAtUtc = nowUtc.plus(Duration.ofSeconds((long) (HOLD_DURATION_MINUTES * 60)));

        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .business(business)
                .turf(turf)
                .customer(customer)
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(priceResult.amount())
                .status(BookingStatus.HOLD)
                .bookingSource(BookingSource.WHATSAPP_AI)
                .build();

        try {
            booking = bookingRepository.saveAndFlush(booking);

            BookingHold hold = BookingHold.builder()
                    .booking(booking)
                    .expiresAt(expiresAtUtc)
                    .status(HoldStatus.ACTIVE)
                    .build();

            hold = bookingHoldRepository.save(hold);

            // Audit log
            logAudit(booking, null, BookingStatus.HOLD, customer, "Booking hold created");

            return BookingHoldResponse.builder()
                    .holdId(hold.getId())
                    .bookingId(booking.getId())
                    .bookingNumber(booking.getBookingNumber())
                    .price(booking.getPrice())
                    .expiresAt(hold.getExpiresAt())
                    .status(hold.getStatus())
                    .build();

        } catch (DataIntegrityViolationException ex) {
            log.warn("Double booking caught by DB partial unique index constraint for turf {} on {} {}-{}",
                    turf.getId(), request.getBookingDate(), request.getStartTime(), request.getEndTime());
            throw new SlotUnavailableException();
        }
    }

    /**
     * Confirms a booking on successful payment.
     * Includes late-payment grace period handling (ADR-016).
     */
    @Transactional
    public BookingResponse confirmBooking(UUID bookingId, String gatewayPaymentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return mapToBookingResponse(booking);
        }

        Instant nowUtc = Instant.now();
        Optional<BookingHold> holdOpt = bookingHoldRepository.findByBookingId(bookingId);

        if (booking.getStatus() == BookingStatus.EXPIRED) {
            // ADR-016 Grace Period check: if expired within 60s, attempt to re-acquire slot
            if (holdOpt.isPresent()) {
                BookingHold hold = holdOpt.get();
                Duration expiredAgo = Duration.between(hold.getExpiresAt(), nowUtc);
                if (expiredAgo.getSeconds() <= PAYMENT_GRACE_PERIOD_SECONDS) {
                    boolean isFree = isSlotFreeForReactivation(booking);
                    if (isFree) {
                        log.info("Grace period reactivation triggered for booking {} (Expired {}s ago)", booking.getBookingNumber(), expiredAgo.getSeconds());
                        hold.setStatus(HoldStatus.CONVERTED);
                        bookingHoldRepository.save(hold);

                        BookingStatus oldStatus = booking.getStatus();
                        booking.setStatus(BookingStatus.CONFIRMED);
                        bookingRepository.save(booking);

                        logAudit(booking, oldStatus, BookingStatus.CONFIRMED, null, "Confirmed via payment grace period reactivation. Payment ID: " + gatewayPaymentId);
                        return mapToBookingResponse(booking);
                    }
                }
            }
            throw new HoldExpiredException("Booking hold has expired. Slot is no longer available.");
        }

        if (booking.getStatus() != BookingStatus.HOLD && booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "Cannot confirm booking in status: " + booking.getStatus()) {};
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        if (holdOpt.isPresent()) {
            BookingHold hold = holdOpt.get();
            hold.setStatus(HoldStatus.CONVERTED);
            bookingHoldRepository.save(hold);
        }

        logAudit(booking, oldStatus, BookingStatus.CONFIRMED, null, "Confirmed via payment. Payment ID: " + gatewayPaymentId);
        sendConfirmationWhatsAppNotification(booking);
        return mapToBookingResponse(booking);
    }

    private void sendConfirmationWhatsAppNotification(Booking booking) {
        try {
            if (booking.getCustomer() != null && booking.getCustomer().getPhone() != null) {
                String phone = booking.getCustomer().getPhone();
                String confirmMsg = String.format(
                        "🎉 *BOOKING CONFIRMED!* ⚽\n\n" +
                        "• *Booking Ref:* %s\n" +
                        "• *Turf:* %s\n" +
                        "• *Date:* %s\n" +
                        "• *Time:* %s - %s\n" +
                        "• *Amount Paid:* ₹%s\n" +
                        "• *Status:* CONFIRMED ✅\n\n" +
                        "Thank you for booking with %s! We look forward to hosting you.",
                        booking.getBookingNumber(),
                        booking.getTurf() != null ? booking.getTurf().getName() : "Green Pitch Turf",
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getPrice(),
                        booking.getBusiness() != null ? booking.getBusiness().getName() : "Green Pitch Kolhapur"
                );
                whatsAppService.sendTextMessage(phone, confirmMsg);
                log.info("Sent automated WhatsApp confirmation message to customer {} for booking {}", phone, booking.getBookingNumber());
            }
        } catch (Exception ex) {
            log.error("Failed to send automated WhatsApp confirmation message for booking {}", booking.getBookingNumber(), ex);
        }
    }

    /**
     * Cancels a booking according to business rules (ADR-010).
     */
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, CancelBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return mapToBookingResponse(booking);
        }

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new CancellationDeniedException("Completed or No-Show bookings cannot be cancelled.");
        }

        User requestingUser = userRepository.findById(request.getRequestingUserId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + request.getRequestingUserId()) {});

        Business business = booking.getBusiness();
        ZoneId businessZone = ZoneId.of(business.getTimezone());
        ZonedDateTime nowInBusiness = ZonedDateTime.now(businessZone);

        // Cancellation window rule (ADR-010): Customer can cancel if >= 2 hours before start
        if (requestingUser.getRole() == UserRole.CUSTOMER) {
            if (!requestingUser.getId().equals(booking.getCustomer().getId())) {
                throw new BaseException(ErrorCode.UNAUTHORIZED_BUSINESS_ACCESS, "Customers cannot cancel another user's booking.") {};
            }

            LocalDateTime slotStartLdt = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
            ZonedDateTime slotStartZdt = slotStartLdt.atZone(businessZone);
            Duration leadTime = Duration.between(nowInBusiness, slotStartZdt);

            if (leadTime.toMinutes() < (CANCELLATION_WINDOW_HOURS * 60)) {
                throw new CancellationDeniedException("Cancellation denied. Bookings can only be cancelled at least 2 hours before start time.");
            }
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setCancelledBy(requestingUser);
        bookingRepository.save(booking);

        logAudit(booking, oldStatus, BookingStatus.CANCELLED, requestingUser, request.getReason() != null ? request.getReason() : "Cancelled by user");

        return mapToBookingResponse(booking);
    }

    /**
     * Marks a confirmed booking as COMPLETED (ADR-013).
     */
    @Transactional
    public BookingResponse completeBooking(UUID bookingId, UUID ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "Only CONFIRMED bookings can be marked COMPLETED.") {};
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + ownerId) {});

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        logAudit(booking, oldStatus, BookingStatus.COMPLETED, owner, "Marked completed by owner");
        return mapToBookingResponse(booking);
    }

    /**
     * Marks a confirmed booking as NO_SHOW (ADR-013).
     */
    @Transactional
    public BookingResponse markNoShow(UUID bookingId, UUID ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "Only CONFIRMED bookings can be marked NO_SHOW.") {};
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + ownerId) {});

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);

        logAudit(booking, oldStatus, BookingStatus.NO_SHOW, owner, "Marked no-show by owner");
        return mapToBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public AlternativeSlotsResponse suggestAlternativeSlots(UUID turfId, LocalDate date) {
        DaySlotsResponse daySlots = slotService.getAvailableSlots(turfId, date);
        List<SlotResponse> availableSlots = daySlots.getSlots().stream()
                .filter(SlotResponse::isAvailable)
                .limit(3)
                .toList();

        return AlternativeSlotsResponse.builder()
                .turfId(turfId)
                .date(date)
                .suggestedSlots(availableSlots)
                .build();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with number: " + bookingNumber));
        return mapToBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));
        return mapToBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomer(UUID customerId) {
        return bookingRepository.findByCustomerId(customerId).stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByBusiness(UUID businessId, LocalDate date) {
        return bookingRepository.findByBusinessIdAndBookingDate(businessId, date).stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    private boolean isSlotFreeForReactivation(Booking booking) {
        List<Booking> activeOnSlot = bookingRepository.findByTurfIdAndBookingDateAndStatusIn(
                booking.getTurf().getId(), booking.getBookingDate(),
                List.of(BookingStatus.HOLD, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED));

        return activeOnSlot.stream()
                .filter(b -> !b.getId().equals(booking.getId()))
                .noneMatch(b -> slotService.timesOverlap(booking.getStartTime(), booking.getEndTime(), b.getStartTime(), b.getEndTime()));
    }

    private String generateBookingNumber(int year) {
        Number seqVal;
        try {
            seqVal = (Number) entityManager.createNativeQuery("SELECT nextval('booking_number_seq')").getSingleResult();
        } catch (Exception ex) {
            seqVal = System.currentTimeMillis() % 100000;
        }
        return String.format("BK-%d-%05d", year, seqVal.longValue());
    }

    private void logAudit(Booking booking, BookingStatus oldStatus, BookingStatus newStatus, User changedBy, String reason) {
        BookingAudit audit = BookingAudit.builder()
                .booking(booking)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .build();
        bookingAuditRepository.save(audit);
    }

    public BookingResponse mapToBookingResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingNumber(b.getBookingNumber())
                .businessId(b.getBusiness().getId())
                .turfId(b.getTurf().getId())
                .customerId(b.getCustomer().getId())
                .bookingDate(b.getBookingDate())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .price(b.getPrice())
                .status(b.getStatus())
                .bookingSource(b.getBookingSource())
                .cancelledAt(b.getCancelledAt())
                .cancelledBy(b.getCancelledBy() != null ? b.getCancelledBy().getId() : null)
                .createdAt(b.getCreatedAt())
                .build();
    }
}
