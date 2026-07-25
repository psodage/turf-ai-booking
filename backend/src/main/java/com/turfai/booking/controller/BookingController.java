package com.turfai.booking.controller;

import com.turfai.booking.dto.request.CancelBookingRequest;
import com.turfai.booking.dto.request.ConfirmBookingRequest;
import com.turfai.booking.dto.request.CreateBookingHoldRequest;
import com.turfai.booking.dto.response.BookingHoldResponse;
import com.turfai.booking.dto.response.BookingResponse;
import com.turfai.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Engine API", description = "Endpoints for booking holds, payment confirmations, cancellations, and history.")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/holds")
    @Operation(summary = "Create a 10-minute temporary booking hold")
    public ResponseEntity<BookingHoldResponse> createBookingHold(@Valid @RequestBody CreateBookingHoldRequest request) {
        BookingHoldResponse response = bookingService.createBookingHold(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking upon successful payment webhook")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmBookingRequest request) {
        BookingResponse response = bookingService.confirmBooking(id, request.getGatewayPaymentId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking according to 2-hour customer window or owner override")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID id,
            @Valid @RequestBody CancelBookingRequest request) {
        BookingResponse response = bookingService.cancelBooking(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingNumber}")
    @Operation(summary = "Get booking details by human-readable booking number")
    public ResponseEntity<BookingResponse> getBookingByNumber(@PathVariable String bookingNumber) {
        BookingResponse response = bookingService.getBookingByNumber(bookingNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List all bookings for a specific customer")
    public ResponseEntity<List<BookingResponse>> getBookingsByCustomer(@PathVariable UUID customerId) {
        List<BookingResponse> response = bookingService.getBookingsByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/business/{businessId}")
    @Operation(summary = "List all bookings for a business on a specific date")
    public ResponseEntity<List<BookingResponse>> getBookingsByBusiness(
            @PathVariable UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<BookingResponse> response = bookingService.getBookingsByBusiness(businessId, date);
        return ResponseEntity.ok(response);
    }
}
