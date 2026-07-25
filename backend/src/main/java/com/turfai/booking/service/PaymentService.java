package com.turfai.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.config.RazorpayProperties;
import com.turfai.booking.dto.payment.CreatePaymentLinkRequest;
import com.turfai.booking.dto.payment.PaymentResponse;
import com.turfai.booking.dto.payment.RazorpayWebhookPayload;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Payment;
import com.turfai.booking.entity.PaymentAudit;
import com.turfai.booking.entity.PaymentGateway;
import com.turfai.booking.entity.PaymentStatus;
import com.turfai.booking.entity.RefundStatus;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.exception.BookingNotFoundException;
import com.turfai.booking.exception.ErrorCode;
import com.turfai.booking.exception.HoldExpiredException;
import com.turfai.booking.exception.WebhookSignatureException;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.PaymentAuditRepository;
import com.turfai.booking.repository.PaymentRepository;
import com.turfai.booking.service.payment.PaymentLinkDto;
import com.turfai.booking.service.payment.RazorpayClientWrapper;
import com.turfai.booking.service.payment.RefundResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private final RazorpayProperties razorpayProperties;
    private final RazorpayClientWrapper razorpayClientWrapper;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAuditRepository paymentAuditRepository;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse createPaymentLink(CreatePaymentLinkRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + request.getBookingId()));

        if (booking.getStatus() != BookingStatus.HOLD && booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, "Payment link can only be generated for bookings in HOLD status.") {};
        }

        // Check for active existing payment link (Idempotency - prevents duplicate click links)
        List<Payment> existingPayments = paymentRepository.findByBookingId(booking.getId());
        Optional<Payment> activePaymentOpt = existingPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.CREATED)
                .findFirst();

        if (activePaymentOpt.isPresent()) {
            log.info("Returning existing active payment link for booking {}", booking.getBookingNumber());
            return mapToPaymentResponse(activePaymentOpt.get());
        }

        String description = "Turf booking for " + booking.getTurf().getName() + " on " + booking.getBookingDate() + " (" + booking.getStartTime() + ")";
        String customerName = booking.getCustomer().getName();
        String customerPhone = booking.getCustomer().getPhone();

        PaymentLinkDto linkDto = razorpayClientWrapper.createPaymentLink(
                booking.getPrice(),
                description,
                customerName,
                customerPhone,
                booking.getBookingNumber()
        );

        Payment payment = Payment.builder()
                .booking(booking)
                .business(booking.getBusiness())
                .customer(booking.getCustomer())
                .amount(booking.getPrice())
                .currency("INR")
                .gateway(PaymentGateway.RAZORPAY)
                .status(PaymentStatus.PENDING)
                .gatewayPaymentId(linkDto.getLinkId())
                .build();

        payment = paymentRepository.save(payment);

        logPaymentAudit(payment, "LINK_GENERATED", "Payment link generated: " + linkDto.getShortUrl());
        PaymentResponse resp = mapToPaymentResponse(payment);
        resp.setPaymentUrl(linkDto.getShortUrl());
        return resp;
    }

    @Transactional
    public void processRazorpayWebhook(String rawPayload, String signatureHeader) {
        // 1. Signature Verification
        verifyWebhookSignature(rawPayload, signatureHeader);

        try {
            RazorpayWebhookPayload payload = objectMapper.readValue(rawPayload, RazorpayWebhookPayload.class);
            String event = payload.getEvent();
            log.info("Received Razorpay webhook event: {}", event);

            Map<String, Object> eventData = payload.getPayload();
            if (eventData == null) return;

            String linkId = extractLinkId(eventData);
            if (linkId == null) {
                log.warn("Webhook event payload does not contain payment link ID.");
                return;
            }

            Optional<Payment> paymentOpt = paymentRepository.findByGatewayPaymentId(linkId);
            if (paymentOpt.isEmpty()) {
                log.warn("No Payment entity found for Razorpay link ID: {}", linkId);
                return;
            }

            Payment payment = paymentOpt.get();

            // 2. Event Deduplication Check
            if (paymentAuditRepository.existsByPaymentIdAndEvent(payment.getId(), event)) {
                log.info("Ignoring duplicate Razorpay webhook event: {} for payment {}", event, payment.getId());
                return;
            }

            // 3. Handle Webhook Events
            if ("payment.link.paid".equals(event) || "payment.captured".equals(event)) {
                handlePaymentSuccess(payment, eventData, event);
            } else if ("payment.failed".equals(event)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                logPaymentAudit(payment, event, "Payment attempt failed.");
            } else if ("payment.link.expired".equals(event) || "payment.link.cancelled".equals(event)) {
                payment.setStatus(PaymentStatus.EXPIRED);
                paymentRepository.save(payment);
                logPaymentAudit(payment, event, "Payment link expired or cancelled.");
            }

        } catch (Exception ex) {
            log.error("Error processing Razorpay webhook payload", ex);
            throw new RuntimeException("Webhook processing error: " + ex.getMessage(), ex);
        }
    }

    private void handlePaymentSuccess(Payment payment, Map<String, Object> eventData, String event) {
        String paymentId = extractPaymentId(eventData);

        payment.setStatus(PaymentStatus.SUCCESS);
        if (paymentId != null) {
            payment.setGatewayPaymentId(paymentId);
        }
        paymentRepository.save(payment);

        try {
            // Attempt booking confirmation (includes ADR-016 60s grace period handling)
            bookingService.confirmBooking(payment.getBooking().getId(), paymentId != null ? paymentId : "PAY_LINK_PAID");
            logPaymentAudit(payment, event, "Payment succeeded and booking confirmed.");
        } catch (HoldExpiredException ex) {
            log.warn("Hold expired before payment capture for booking {}. Triggering automatic refund.", payment.getBooking().getBookingNumber());
            payment.setRefundStatus(RefundStatus.REQUESTED);
            paymentRepository.save(payment);

            razorpayClientWrapper.initiateRefund(paymentId != null ? paymentId : payment.getGatewayPaymentId(), payment.getAmount(), "Hold expired before payment completion");
            payment.setRefundStatus(RefundStatus.SUCCESS);
            paymentRepository.save(payment);
            logPaymentAudit(payment, "AUTO_REFUND_TRIGGERED", "Hold expired. Automated refund initiated.");
        }
    }

    @Transactional
    public PaymentResponse initiateRefund(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Payment not found with ID: " + paymentId) {});

        RefundResultDto refundResult = razorpayClientWrapper.initiateRefund(payment.getGatewayPaymentId(), payment.getAmount(), reason);
        payment.setRefundStatus(RefundStatus.SUCCESS);
        paymentRepository.save(payment);

        logPaymentAudit(payment, "REFUND_COMPLETED", "Refund processed. Refund ID: " + refundResult.getRefundId());
        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Payment not found with ID: " + paymentId) {});
        return mapToPaymentResponse(payment);
    }

    public void verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new WebhookSignatureException("Missing X-Razorpay-Signature header");
        }

        String calculatedSignature = calculateHmacSha256(rawPayload, razorpayProperties.getWebhookSecret());
        if (!MessageDigest.isEqual(signatureHeader.getBytes(StandardCharsets.UTF_8), calculatedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new WebhookSignatureException("Invalid Razorpay webhook signature");
        }
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new WebhookSignatureException("Failed to calculate HMAC signature");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractLinkId(Map<String, Object> eventData) {
        if (eventData.containsKey("payment_link")) {
            Map<String, Object> linkObj = (Map<String, Object>) eventData.get("payment_link");
            Map<String, Object> entity = (Map<String, Object>) linkObj.get("entity");
            return entity != null ? (String) entity.get("id") : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractPaymentId(Map<String, Object> eventData) {
        if (eventData.containsKey("payment")) {
            Map<String, Object> paymentObj = (Map<String, Object>) eventData.get("payment");
            Map<String, Object> entity = (Map<String, Object>) paymentObj.get("entity");
            return entity != null ? (String) entity.get("id") : null;
        }
        return null;
    }

    private void logPaymentAudit(Payment payment, String event, String notes) {
        PaymentAudit audit = PaymentAudit.builder()
                .payment(payment)
                .event(event)
                .gatewayPayload(notes)
                .build();
        paymentAuditRepository.save(audit);
    }

    private PaymentResponse mapToPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getId())
                .bookingId(p.getBooking().getId())
                .bookingNumber(p.getBooking().getBookingNumber())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .gatewayPaymentId(p.getGatewayPaymentId())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
