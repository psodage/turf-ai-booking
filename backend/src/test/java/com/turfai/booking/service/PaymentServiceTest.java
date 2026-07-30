package com.turfai.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turfai.booking.config.RazorpayProperties;
import com.turfai.booking.dto.payment.CreatePaymentLinkRequest;
import com.turfai.booking.dto.payment.PaymentResponse;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Payment;
import com.turfai.booking.entity.PaymentStatus;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.exception.WebhookSignatureException;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.PaymentAuditRepository;
import com.turfai.booking.repository.PaymentRepository;
import com.turfai.booking.service.payment.PaymentLinkDto;
import com.turfai.booking.service.payment.RazorpayClientWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private RazorpayProperties razorpayProperties;
    @Mock private RazorpayClientWrapper razorpayClientWrapper;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAuditRepository paymentAuditRepository;
    @Mock private BookingService bookingService;

    private PaymentService paymentService;

    private Business testBusiness;
    private User testCustomer;
    private Turf testTurf;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                razorpayProperties,
                razorpayClientWrapper,
                bookingRepository,
                paymentRepository,
                paymentAuditRepository,
                bookingService,
                new ObjectMapper()
        );

        testBusiness = Business.builder().name("Green Pitch Kolhapur").build();
        testBusiness.setId(UUID.randomUUID());

        testCustomer = User.builder().name("Customer").phone("+919876543210").role(UserRole.CUSTOMER).build();
        testCustomer.setId(UUID.randomUUID());

        testTurf = Turf.builder().name("Turf 1").business(testBusiness).build();
        testTurf.setId(UUID.randomUUID());

        testBooking = Booking.builder()
                .bookingNumber("BK-2026-00001")
                .business(testBusiness)
                .turf(testTurf)
                .customer(testCustomer)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .price(BigDecimal.valueOf(800))
                .status(BookingStatus.HOLD)
                .build();
        testBooking.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create payment link and save payment entity")
    void testCreatePaymentLinkSuccess() {
        CreatePaymentLinkRequest req = CreatePaymentLinkRequest.builder().bookingId(testBooking.getId()).build();

        when(bookingRepository.findById(testBooking.getId())).thenReturn(Optional.of(testBooking));
        when(paymentRepository.findByBookingId(testBooking.getId())).thenReturn(List.of());

        PaymentLinkDto mockLink = PaymentLinkDto.builder()
                .linkId("plink_12345")
                .shortUrl("https://rzp.io/i/plink_12345")
                .amount(BigDecimal.valueOf(800))
                .status("created")
                .build();

        when(razorpayClientWrapper.createPaymentLink(any(), any(), any(), any(), any(), anyLong())).thenReturn(mockLink);
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PaymentResponse response = paymentService.createPaymentLink(req);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentUrl()).isEqualTo("https://rzp.io/i/plink_12345");
        verify(paymentRepository).save(any());
    }

    @Test
    @DisplayName("Should return existing payment link on duplicate click request")
    void testCreatePaymentLinkDuplicateClickReuse() {
        CreatePaymentLinkRequest req = CreatePaymentLinkRequest.builder().bookingId(testBooking.getId()).build();

        Payment existingPayment = Payment.builder()
                .booking(testBooking)
                .business(testBusiness)
                .customer(testCustomer)
                .amount(BigDecimal.valueOf(800))
                .status(PaymentStatus.PENDING)
                .gatewayPaymentId("plink_existing")
                .build();
        existingPayment.setId(UUID.randomUUID());

        when(bookingRepository.findById(testBooking.getId())).thenReturn(Optional.of(testBooking));
        when(paymentRepository.findByBookingId(testBooking.getId())).thenReturn(List.of(existingPayment));

        PaymentResponse response = paymentService.createPaymentLink(req);

        assertThat(response).isNotNull();
        verify(razorpayClientWrapper, never()).createPaymentLink(any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("Should throw WebhookSignatureException when signature does not match")
    void testVerifyWebhookSignatureInvalid() {
        when(razorpayProperties.getWebhookSecret()).thenReturn("correct_secret");

        assertThatThrownBy(() -> paymentService.verifyWebhookSignature("{\"test\":\"data\"}", "wrong_signature"))
                .isInstanceOf(WebhookSignatureException.class);
    }
}
