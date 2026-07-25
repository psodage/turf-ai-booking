package com.turfai.booking.controller;

import com.turfai.booking.dto.report.DashboardSummaryResponse;
import com.turfai.booking.dto.response.BookingResponse;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.service.BookingService;
import com.turfai.booking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@Tag(name = "Owner Dashboard API", description = "Endpoints for owner administration, dashboard metrics, and booking management.")
public class OwnerDashboardController {

    private final ReportService reportService;
    private final BookingService bookingService;

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get Dashboard KPI Metrics (Bookings, Revenue, Occupancy)")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary(
            @RequestParam UUID businessId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        DashboardSummaryResponse response = reportService.getDashboardSummary(businessId, targetDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/today")
    @Operation(summary = "Get Today's Bookings for Business")
    public ResponseEntity<List<BookingResponse>> getTodayBookings(@RequestParam UUID businessId) {
        List<BookingResponse> response = bookingService.getBookingsByBusiness(businessId, LocalDate.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings/upcoming")
    @Operation(summary = "Get Upcoming Confirmed Bookings for Business")
    public ResponseEntity<List<BookingResponse>> getUpcomingBookings(@RequestParam UUID businessId) {
        List<BookingResponse> allToday = bookingService.getBookingsByBusiness(businessId, LocalDate.now());
        List<BookingResponse> upcoming = allToday.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.HOLD)
                .toList();
        return ResponseEntity.ok(upcoming);
    }

    @PostMapping("/bookings/{id}/complete")
    @Operation(summary = "Owner action: Mark booking as COMPLETED")
    public ResponseEntity<BookingResponse> completeBooking(
            @PathVariable UUID id,
            @RequestParam UUID ownerId) {
        BookingResponse response = bookingService.completeBooking(id, ownerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{id}/no-show")
    @Operation(summary = "Owner action: Mark booking as NO_SHOW")
    public ResponseEntity<BookingResponse> markNoShow(
            @PathVariable UUID id,
            @RequestParam UUID ownerId) {
        BookingResponse response = bookingService.markNoShow(id, ownerId);
        return ResponseEntity.ok(response);
    }
}
