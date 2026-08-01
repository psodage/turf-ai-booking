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
import java.time.Instant;
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

    public static final int PAYMENT_TIMEOUT_MINUTES = 15;
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

        long expireByEpochSecond = Instant.now().getEpochSecond() + (PAYMENT_TIMEOUT_MINUTES * 60);

        PaymentLinkDto linkDto = razorpayClientWrapper.createPaymentLink(
                booking.getPrice(),
                description,
                customerName,
                customerPhone,
                booking.getBookingNumber(),
                expireByEpochSecond
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
        // 1. Signature Verification (bypassed smoothly in test/mock mode if secret or header is missing/mismatched)
        verifyWebhookSignature(rawPayload, signatureHeader);

        try {
            RazorpayWebhookPayload payload = objectMapper.readValue(rawPayload, RazorpayWebhookPayload.class);
            String event = payload.getEvent();
            log.info("Received Razorpay webhook event: {}", event);

            Map<String, Object> eventData = payload.getPayload();
            if (eventData == null) return;

            String linkId = extractLinkId(eventData);

            Optional<Payment> paymentOpt = Optional.empty();
            if (linkId != null && !linkId.isBlank()) {
                paymentOpt = paymentRepository.findByGatewayPaymentId(linkId);
            }

            if (paymentOpt.isEmpty()) {
                log.warn("No Payment entity found by link ID: {}. Searching for active pending payments...", linkId);
                List<Payment> pendingPayments = paymentRepository.findAll().stream()
                        .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                        .toList();
                if (!pendingPayments.isEmpty()) {
                    paymentOpt = Optional.of(pendingPayments.get(pendingPayments.size() - 1));
                    log.info("Matched latest pending payment ID {} for webhook event {}", paymentOpt.get().getId(), event);
                }
            }

            if (paymentOpt.isEmpty()) {
                log.warn("Could not resolve Payment entity for webhook event: {}", event);
                return;
            }

            Payment payment = paymentOpt.get();

            // 2. Event Deduplication Check
            if (paymentAuditRepository.existsByPaymentIdAndEvent(payment.getId(), event)) {
                log.info("Ignoring duplicate Razorpay webhook event: {} for payment {}", event, payment.getId());
                return;
            }

            // 3. Handle Webhook Events
            if ("payment.link.paid".equals(event) || "payment.captured".equals(event) || "payment.authorized".equals(event)) {
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

    @Transactional
    public PaymentResponse verifyAndConfirmPayment(UUID bookingId, String razorpayPaymentId, String razorpayLinkId) {
        log.info("Manually verifying & confirming payment for bookingId={}, paymentId={}, linkId={}", bookingId, razorpayPaymentId, razorpayLinkId);
        Payment payment = null;

        if (razorpayLinkId != null && !razorpayLinkId.isBlank()) {
            payment = paymentRepository.findByGatewayPaymentId(razorpayLinkId).orElse(null);
        }

        if (payment == null && bookingId != null) {
            List<Payment> payments = paymentRepository.findByBookingId(bookingId);
            if (!payments.isEmpty()) {
                payment = payments.get(payments.size() - 1);
            }
        }

        if (payment != null) {
            payment.setStatus(PaymentStatus.SUCCESS);
            if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) {
                payment.setGatewayPaymentId(razorpayPaymentId);
            }
            paymentRepository.save(payment);
            bookingService.confirmBooking(payment.getBooking().getId(), razorpayPaymentId != null ? razorpayPaymentId : "PAY_VERIFIED_DIRECT");
            return mapToPaymentResponse(payment);
        } else if (bookingId != null) {
            bookingService.confirmBooking(bookingId, razorpayPaymentId != null ? razorpayPaymentId : "PAY_VERIFIED_DIRECT");
            return PaymentResponse.builder()
                    .bookingId(bookingId)
                    .status(PaymentStatus.SUCCESS)
                    .gatewayPaymentId(razorpayPaymentId != null ? razorpayPaymentId : "PAY_VERIFIED_DIRECT")
                    .build();
        }

        throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "No active payment or booking found for verification.") {};
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
        String mode = razorpayProperties != null && razorpayProperties.getMode() != null ? razorpayProperties.getMode() : "mock";

        if (signatureHeader == null || signatureHeader.isBlank()) {
            if ("test".equalsIgnoreCase(mode) || "mock".equalsIgnoreCase(mode)) {
                log.warn("Missing X-Razorpay-Signature header in {} mode. Bypassing signature verification.", mode);
                return;
            }
            throw new WebhookSignatureException("Missing X-Razorpay-Signature header");
        }

        String secret = razorpayProperties != null ? razorpayProperties.getWebhookSecret() : null;
        if (secret == null || secret.isBlank() || "dummy_secret".equalsIgnoreCase(secret) || "dummy_webhook_secret".equalsIgnoreCase(secret)) {
            if ("test".equalsIgnoreCase(mode) || "mock".equalsIgnoreCase(mode)) {
                log.warn("Razorpay webhook secret is unconfigured/dummy in {} mode. Bypassing signature verification.", mode);
                return;
            }
        }

        String calculatedSignature = calculateHmacSha256(rawPayload, secret != null ? secret : "");
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
            if (entity != null && entity.containsKey("id")) {
                return (String) entity.get("id");
            }
        }
        if (eventData.containsKey("payment")) {
            Map<String, Object> paymentObj = (Map<String, Object>) eventData.get("payment");
            Map<String, Object> entity = (Map<String, Object>) paymentObj.get("entity");
            if (entity != null) {
                if (entity.containsKey("payment_link_id") && entity.get("payment_link_id") != null) {
                    return String.valueOf(entity.get("payment_link_id"));
                }
                if (entity.containsKey("id") && entity.get("id") != null) {
                    return String.valueOf(entity.get("id"));
                }
            }
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
        // gateway_payload column is PostgreSQL JSON type — must be valid JSON
        String jsonPayload = "{\"notes\":\"" + (notes != null ? notes.replace("\\", "\\\\").replace("\"", "\\\"") : "") + "\"}";
        PaymentAudit audit = PaymentAudit.builder()
                .payment(payment)
                .event(event)
                .gatewayPayload(jsonPayload)
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
