package com.turfai.booking.ai.tool;

import com.turfai.booking.dto.request.BlockSlotRequest;
import com.turfai.booking.dto.request.CancelBookingRequest;
import com.turfai.booking.dto.request.CreateBookingHoldRequest;
import com.turfai.booking.dto.response.AlternativeSlotsResponse;
import com.turfai.booking.dto.response.BlockedSlotResponse;
import com.turfai.booking.dto.response.BookingHoldResponse;
import com.turfai.booking.dto.response.BookingResponse;
import com.turfai.booking.dto.response.DaySlotsResponse;
import com.turfai.booking.entity.BlockReason;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.User;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.service.BlockedSlotService;
import com.turfai.booking.service.BookingService;
import com.turfai.booking.service.PaymentService;
import com.turfai.booking.service.PricingService;
import com.turfai.booking.service.ReportService;
import com.turfai.booking.service.SlotService;
import com.turfai.booking.service.TurfService;
import com.turfai.booking.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolGateway {

    private final SlotService slotService;
    private final PricingService pricingService;
    private final BookingService bookingService;
    private final BlockedSlotService blockedSlotService;
    private final TurfService turfService;
    private final BookingRepository bookingRepository;
    private final PaymentService paymentService;
    private final ReportService reportService;
    private final WhatsAppService whatsAppService;

    // getLocation
    public ToolResult getLocation(Business business) {
        try {
            if (business == null) {
                return ToolResult.error("BUSINESS_NOT_FOUND", "Business information is not available.", null);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("name", business.getName());
            data.put("address", business.getAddress() != null ? business.getAddress() : "Near Rankala Lake, Ring Road, Kolhapur, Maharashtra (416012)");
            data.put("google_maps_link", business.getGoogleMapsLink());
            if (business.getLatitude() != null && business.getLongitude() != null) {
                data.put("latitude", business.getLatitude());
                data.put("longitude", business.getLongitude());
                data.put("has_native_location", true);
            } else {
                data.put("has_native_location", false);
            }
            return ToolResult.success("Location retrieved successfully.", data);
        } catch (Exception ex) {
            log.error("Error in getLocation tool", ex);
            return ToolResult.error("INTERNAL_ERROR", "Failed to retrieve location.", null);
        }
    }

    // getUserBookings
    public ToolResult getUserBookings(User customer, String inputPhone) {
        try {
            List<Booking> bookings = List.of();
            String searchPhone = inputPhone;

            if (searchPhone != null && !searchPhone.isBlank()) {
                String altPhone = searchPhone.startsWith("+") ? searchPhone.substring(1) : "+" + searchPhone;
                bookings = bookingRepository.findByCustomerPhoneWithDetails(searchPhone, altPhone);
            }

            if (bookings.isEmpty() && customer != null && customer.getId() != null) {
                bookings = bookingRepository.findByCustomerIdWithDetails(customer.getId());
            }

            if (bookings.isEmpty() && customer != null && customer.getPhone() != null) {
                String phone = customer.getPhone();
                String altPhone = phone.startsWith("+") ? phone.substring(1) : "+" + phone;
                bookings = bookingRepository.findByCustomerPhoneWithDetails(phone, altPhone);
            }

            if (bookings.isEmpty()) {
                Map<String, Object> emptyData = new HashMap<>();
                emptyData.put("found", false);
                emptyData.put("bookings", List.of());
                return ToolResult.success("No bookings found.", emptyData);
            }

            List<Map<String, Object>> bookingList = bookings.stream().map(b -> {
                Map<String, Object> m = new HashMap<>();
                m.put("booking_id", b.getBookingNumber());
                m.put("date", b.getBookingDate().toString());
                m.put("time_slot", b.getStartTime() + " - " + b.getEndTime());
                m.put("turf_name", b.getTurf() != null ? b.getTurf().getName() : "Green Pitch Turf");
                m.put("status", b.getStatus().name());
                m.put("amount_paid", b.getPrice());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("found", true);
            resultData.put("count", bookingList.size());
            resultData.put("bookings", bookingList);

            return ToolResult.success("Bookings retrieved successfully.", resultData);
        } catch (Exception ex) {
            log.error("Error in getUserBookings tool", ex);
            return ToolResult.error("INTERNAL_ERROR", "Failed to retrieve booking details.", null);
        }
    }

    // 1. checkAvailability
    public ToolResult checkAvailability(UUID turfId, LocalDate date) {
        try {
            DaySlotsResponse response = slotService.getAvailableSlots(turfId, date);
            return ToolResult.success(response);
        } catch (BaseException ex) {
            return ToolResult.error(ex.getErrorCode().name(), ex.getMessage(), List.of("Try another date within the 30-day advance window."));
        } catch (Exception ex) {
            log.error("Error in checkAvailability tool", ex);
            return ToolResult.error("INTERNAL_ERROR", "Failed to query slot availability.", List.of("Please try again."));
        }
    }

    // 2. getPricing
    public ToolResult getPricing(UUID turfId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        try {
            PricingService.PriceResult priceResult = pricingService.calculateSlotPrice(turfId, date, startTime, endTime);

            Map<String, Object> data = new HashMap<>();
            data.put("turf_id", turfId);
            data.put("date", date.toString());
            data.put("start_time", startTime.toString());
            data.put("end_time", endTime.toString());
            data.put("hourly_rate", priceResult.amount());
            data.put("pricing_type", priceResult.type().name());

            return ToolResult.success(data);
        } catch (BaseException ex) {
            return ToolResult.error(ex.getErrorCode().name(), ex.getMessage(), null);
        } catch (Exception ex) {
            log.error("Error in getPricing tool", ex);
            return ToolResult.error("INTERNAL_ERROR", "Failed to calculate pricing.", null);
        }
    }

    // 3. createBookingHold
    public ToolResult createBookingHold(UUID turfId, UUID customerId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        try {
            CreateBookingHoldRequest req = CreateBookingHoldRequest.builder()
                    .turfId(turfId)
                    .customerId(customerId)
                    .bookingDate(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();

            BookingHoldResponse hold = bookingService.createBookingHold(req);

            // Automatically generate payment link for the hold
            var paymentResp = paymentService.createPaymentLink(
                    com.turfai.booking.dto.payment.CreatePaymentLinkRequest.builder()
                            .bookingId(hold.getBookingId())
                            .build()
            );

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", hold.getBookingId());
            data.put("booking_number", hold.getBookingNumber());
            data.put("status", hold.getStatus().name());
            data.put("expires_at", hold.getExpiresAt().toString());
            data.put("price", hold.getPrice());
            data.put("payment_url", paymentResp.getPaymentUrl());

            return ToolResult.success("Booking hold created! Complete payment within 5 minutes to confirm your booking.", data);
        } catch (BaseException ex) {
            AlternativeSlotsResponse alts = bookingService.suggestAlternativeSlots(turfId, date);
            List<String> altSuggestions = (alts != null && alts.getSuggestedSlots() != null)
                    ? alts.getSuggestedSlots().stream().map(s -> s.getStartTime().toString()).collect(Collectors.toList())
                    : List.of();

            return ToolResult.error(ex.getErrorCode().name(), ex.getMessage(), altSuggestions);
        } catch (Exception ex) {
            log.error("Error in createBookingHold tool", ex);
            return ToolResult.error("HOLD_FAILED", "Could not create booking hold.", null);
        }
    }

    // 4. confirmBooking
    public ToolResult confirmBooking(UUID bookingId, String paymentId) {
        try {
            BookingResponse response = bookingService.confirmBooking(bookingId, paymentId != null ? paymentId : "PAY_DUMMY_DIRECT");
            return ToolResult.success("Booking confirmed successfully!", response);
        } catch (BaseException ex) {
            return ToolResult.error(ex.getErrorCode().name(), ex.getMessage(), List.of("Please contact owner support if payment was deducted."));
        } catch (Exception ex) {
            log.error("Error in confirmBooking tool", ex);
            return ToolResult.error("CONFIRMATION_FAILED", "Failed to confirm booking.", null);
        }
    }

    // 5. cancelBooking
    public ToolResult cancelBooking(UUID bookingId, UUID requestingUserId, String reason) {
        try {
            CancelBookingRequest req = CancelBookingRequest.builder()
                    .requestingUserId(requestingUserId)
                    .reason(reason != null ? reason : "Requested by user via WhatsApp")
                    .build();

            BookingResponse response = bookingService.cancelBooking(bookingId, req);
            return ToolResult.success("Booking cancelled successfully.", response);
        } catch (BaseException ex) {
            return ToolResult.error(ex.getErrorCode().name(), ex.getMessage(), List.of("Cancellations must be requested at least 2 hours prior to slot start time."));
        } catch (Exception ex) {
            log.error("Error in cancelBooking tool", ex);
            return ToolResult.error("CANCELLATION_FAILED", "Could not cancel booking.", null);
        }
    }

    // 6. suggestAlternativeSlots
    public ToolResult suggestAlternativeSlots(UUID turfId, LocalDate date) {
        try {
            AlternativeSlotsResponse alts = bookingService.suggestAlternativeSlots(turfId, date);
            return ToolResult.success(alts);
        } catch (Exception ex) {
            log.error("Error in suggestAlternativeSlots tool", ex);
            return ToolResult.error("INTERNAL_ERROR", "Failed to suggest alternative slots.", null);
        }
    }

    // 7. getBookingStatus
    public ToolResult getBookingStatus(String bookingNumber) {
        try {
            Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingNumber));

            Map<String, Object> data = new HashMap<>();
            data.put("booking_id", booking.getId());
            data.put("booking_number", booking.getBookingNumber());
            data.put("status", booking.getStatus().name());
            data.put("date", booking.getBookingDate().toString());
            data.put("start_time", booking.getStartTime().toString());
            data.put("end_time", booking.getEndTime().toString());
            data.put("price", booking.getPrice());

            return ToolResult.success(data);
        } catch (Exception ex) {
            return ToolResult.error("BOOKING_NOT_FOUND", "No booking found matching " + bookingNumber, List.of("Please verify your booking reference number."));
        }
    }

    // 8. getTodayBookings (Owner Tool)
    public ToolResult getTodayBookings(UUID businessId, LocalDate date) {
        try {
            LocalDate targetDate = date != null ? date : LocalDate.now();
            List<Booking> bookings = bookingRepository.findByBusinessIdAndBookingDate(businessId, targetDate);

            List<Map<String, Object>> list = bookings.stream().map(b -> {
                Map<String, Object> m = new HashMap<>();
                m.put("booking_number", b.getBookingNumber());
                m.put("customer_name", b.getCustomer() != null ? b.getCustomer().getName() : "Unknown");
                m.put("customer_phone", b.getCustomer() != null ? b.getCustomer().getPhone() : "N/A");
                m.put("turf_name", b.getTurf().getName());
                m.put("time", b.getStartTime() + " - " + b.getEndTime());
                m.put("status", b.getStatus().name());
                m.put("amount", b.getPrice());
                return m;
            }).collect(Collectors.toList());

            return ToolResult.success("Bookings for " + targetDate, list);
        } catch (Exception ex) {
            log.error("Error in getTodayBookings tool", ex);
            return ToolResult.error("INTERNAL_ERROR", "Could not fetch bookings.", null);
        }
    }

    // 9. blockSlot (Owner Tool)
    public ToolResult blockSlot(UUID turfId, LocalDate date, LocalTime startTime, LocalTime endTime, BlockReason reason, UUID createdBy) {
        try {
            BlockSlotRequest req = BlockSlotRequest.builder()
                    .turfId(turfId)
                    .date(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .reason(reason != null ? reason : BlockReason.MAINTENANCE)
                    .createdBy(createdBy)
                    .build();

            BlockedSlotResponse response = blockedSlotService.blockSlot(req);
            return ToolResult.success("Slot blocked successfully.", response);
        } catch (BaseException ex) {
            return ToolResult.error(ex.getErrorCode().name(), ex.getMessage(), null);
        } catch (Exception ex) {
            log.error("Error in blockSlot tool", ex);
            return ToolResult.error("BLOCK_FAILED", "Failed to block slot.", null);
        }
    }

    // 10. unblockSlot (Owner Tool)
    public ToolResult unblockSlot(UUID blockedSlotId, UUID ownerId) {
        try {
            blockedSlotService.unblockSlot(blockedSlotId);
            return ToolResult.success("Slot unblocked successfully.", Map.of("blocked_slot_id", blockedSlotId));
        } catch (BaseException ex) {
            return ToolResult.error(ex.getErrorCode().name(), ex.getMessage(), null);
        } catch (Exception ex) {
            log.error("Error in unblockSlot tool", ex);
            return ToolResult.error("UNBLOCK_FAILED", "Failed to unblock slot.", null);
        }
    }

    // 11. getBusinessSummary (Owner Tool)
    public ToolResult getBusinessSummary(UUID businessId, LocalDate date) {
        try {
            LocalDate targetDate = date != null ? date : LocalDate.now();
            List<Booking> bookings = bookingRepository.findByBusinessIdAndBookingDate(businessId, targetDate);

            long totalBookings = bookings.size();
            long confirmedCount = bookings.stream().filter(b -> b.getStatus().name().equals("CONFIRMED")).count();
            BigDecimal totalRevenue = bookings.stream()
                    .filter(b -> b.getStatus().name().equals("CONFIRMED") || b.getStatus().name().equals("COMPLETED"))
                    .map(Booking::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> summary = new HashMap<>();
            summary.put("date", targetDate.toString());
            summary.put("total_bookings", totalBookings);
            summary.put("confirmed_bookings", confirmedCount);
            summary.put("revenue", totalRevenue);

            return ToolResult.success("Business summary for " + targetDate, summary);
        } catch (Exception ex) {
            log.error("Error in getBusinessSummary tool", ex);
            return ToolResult.error("INTERNAL_ERROR", "Could not calculate business summary.", null);
        }
    }

    // 12. generateExcelReport (Owner Tool)
    public ToolResult generateExcelReport(UUID businessId, String reportTypeStr, LocalDate startDate, LocalDate endDate, String ownerPhone) {
        try {
            com.turfai.booking.entity.ReportType reportType;
            try {
                reportType = com.turfai.booking.entity.ReportType.valueOf(reportTypeStr != null ? reportTypeStr.toUpperCase() : "DAILY");
            } catch (Exception e) {
                reportType = com.turfai.booking.entity.ReportType.DAILY;
            }

            var response = reportService.generateReport(com.turfai.booking.dto.report.GenerateReportRequest.builder()
                    .businessId(businessId)
                    .reportType(reportType)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build());

            // If owner phone is provided, send Excel document attachment via WhatsApp
            if (ownerPhone != null && !ownerPhone.isBlank() && whatsAppService != null) {
                whatsAppService.sendDocumentMessage(ownerPhone, response.getDownloadUrl(), response.getFileName(), "Here is your requested " + reportType + " report.");
            }

            return ToolResult.success("Excel report generated successfully.", Map.of(
                    "report_id", response.getReportId(),
                    "file_name", response.getFileName(),
                    "download_url", response.getDownloadUrl(),
                    "file_size", response.getFileSize()
            ));
        } catch (Exception ex) {
            log.error("Error in generateExcelReport tool", ex);
            return ToolResult.error("REPORT_FAILED", "Failed to generate Excel report: " + ex.getMessage(), null);
        }
    }
}
