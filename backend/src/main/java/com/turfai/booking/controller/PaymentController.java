package com.turfai.booking.controller;

import com.turfai.booking.dto.payment.CreatePaymentLinkRequest;
import com.turfai.booking.dto.payment.PaymentResponse;
import com.turfai.booking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management API", description = "Endpoints for payment link generation, lookup, and refund initiation.")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/link")
    @Operation(summary = "Generate Payment Link for Booking Hold")
    public ResponseEntity<PaymentResponse> createPaymentLink(@Valid @RequestBody CreatePaymentLinkRequest request) {
        PaymentResponse response = paymentService.createPaymentLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Lookup Payment Status")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Initiate Refund for Payment")
    public ResponseEntity<PaymentResponse> initiateRefund(
            @PathVariable UUID paymentId,
            @RequestParam(required = false, defaultValue = "Manual owner refund request") String reason) {

        PaymentResponse response = paymentService.initiateRefund(paymentId, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify & Confirm Razorpay Test/Live Payment")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @RequestParam(required = false) UUID bookingId,
            @RequestParam(required = false) String razorpayPaymentId,
            @RequestParam(required = false) String razorpayLinkId) {

        PaymentResponse response = paymentService.verifyAndConfirmPayment(bookingId, razorpayPaymentId, razorpayLinkId);
        return ResponseEntity.ok(response);
    }
}
