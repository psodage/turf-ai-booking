package com.turfai.booking.repository;

import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingHold;
import com.turfai.booking.entity.BookingSource;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationStatus;
import com.turfai.booking.entity.HoldStatus;
import com.turfai.booking.entity.Payment;
import com.turfai.booking.entity.PaymentGateway;
import com.turfai.booking.entity.PaymentStatus;
import com.turfai.booking.entity.RefundStatus;
import com.turfai.booking.entity.SystemSetting;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryCustomQueryTest {

    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TurfRepository turfRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingHoldRepository bookingHoldRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SystemSettingRepository systemSettingRepository;
    @Autowired private ConversationRepository conversationRepository;

    private Business testBusiness;
    private User testCustomer;
    private Turf testTurf;

    @BeforeEach
    void setUp() {
        testBusiness = businessRepository.saveAndFlush(Business.builder()
                .name("Test Business Arena")
                .whatsappPhoneNumberId("PN_TEST_QUERY_99")
                .phone("+919111122222")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build());

        testCustomer = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Query Test User")
                .phone("+919333344444")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        testTurf = turfRepository.saveAndFlush(Turf.builder()
                .business(testBusiness)
                .name("Query Turf")
                .type(TurfType.FIVE_A_SIDE)
                .status(TurfStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("BookingRepository: Should retrieve booking by booking number with JOIN FETCH details")
    void testFindByBookingNumberWithDetails() {
        Long seqVal = bookingRepository.getNextBookingSequenceValue();
        String bookingNumber = "BK-QUERY-" + seqVal;

        Booking booking = bookingRepository.saveAndFlush(Booking.builder()
                .bookingNumber(bookingNumber)
                .business(testBusiness)
                .turf(testTurf)
                .customer(testCustomer)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .price(new BigDecimal("1000.00"))
                .status(BookingStatus.CONFIRMED)
                .bookingSource(BookingSource.WHATSAPP_AI)
                .build());

        Optional<Booking> fetchedOpt = bookingRepository.findByBookingNumberWithDetails(bookingNumber);
        assertThat(fetchedOpt).isPresent();
        Booking fetched = fetchedOpt.get();
        assertThat(fetched.getTurf().getName()).isEqualTo("Query Turf");
        assertThat(fetched.getCustomer().getName()).isEqualTo("Query Test User");
        assertThat(fetched.getBusiness().getName()).isEqualTo("Test Business Arena");
    }

    @Test
    @DisplayName("BookingHoldRepository: Should find active expired holds by status and expiresAtBefore")
    void testFindExpiredHolds() {
        Long seqVal = bookingRepository.getNextBookingSequenceValue();
        String bookingNumber = "BK-HOLD-" + seqVal;

        Booking booking = bookingRepository.saveAndFlush(Booking.builder()
                .bookingNumber(bookingNumber)
                .business(testBusiness)
                .turf(testTurf)
                .customer(testCustomer)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .price(new BigDecimal("1000.00"))
                .status(BookingStatus.HOLD)
                .bookingSource(BookingSource.WHATSAPP_AI)
                .build());

        BookingHold expiredHold = bookingHoldRepository.saveAndFlush(BookingHold.builder()
                .booking(booking)
                .status(HoldStatus.ACTIVE)
                .expiresAt(Instant.now().minusSeconds(120)) // expired 2 mins ago
                .build());

        List<BookingHold> expiredHolds = bookingHoldRepository.findByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, Instant.now());
        assertThat(expiredHolds).extracting(BookingHold::getId).contains(expiredHold.getId());
    }

    @Test
    @DisplayName("PaymentRepository: Should find payment by Razorpay Gateway Order ID and Payment ID")
    void testPaymentQueries() {
        Long seqVal = bookingRepository.getNextBookingSequenceValue();
        String bookingNumber = "BK-PAY-" + seqVal;

        Booking booking = bookingRepository.saveAndFlush(Booking.builder()
                .bookingNumber(bookingNumber)
                .business(testBusiness)
                .turf(testTurf)
                .customer(testCustomer)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(17, 0))
                .price(new BigDecimal("1000.00"))
                .status(BookingStatus.PAYMENT_PENDING)
                .bookingSource(BookingSource.WHATSAPP_AI)
                .build());

        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .booking(booking)
                .business(testBusiness)
                .customer(testCustomer)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .gateway(PaymentGateway.RAZORPAY)
                .gatewayOrderId("order_query_999")
                .gatewayPaymentId("pay_query_888")
                .status(PaymentStatus.SUCCESS)
                .refundStatus(RefundStatus.NOT_REQUIRED)
                .build());

        Optional<Payment> byOrderId = paymentRepository.findByGatewayOrderId("order_query_999");
        assertThat(byOrderId).isPresent();
        assertThat(byOrderId.get().getId()).isEqualTo(payment.getId());

        Optional<Payment> byPaymentId = paymentRepository.findByGatewayPaymentId("pay_query_888");
        assertThat(byPaymentId).isPresent();
        assertThat(byPaymentId.get().getId()).isEqualTo(payment.getId());
    }

    @Test
    @DisplayName("SystemSettingRepository: Should find system setting by key using findById")
    void testFindBySettingKey() {
        Optional<SystemSetting> settingOpt = systemSettingRepository.findById("HOLD_DURATION_MINUTES");
        assertThat(settingOpt).isPresent();
        assertThat(settingOpt.get().getValue()).isEqualTo("10");
    }

    @Test
    @DisplayName("ConversationRepository: Should find active conversation by user ID and business ID")
    void testConversationQueries() {
        Conversation conversation = conversationRepository.saveAndFlush(Conversation.builder()
                .user(testCustomer)
                .business(testBusiness)
                .role(UserRole.CUSTOMER)
                .status(ConversationStatus.ACTIVE)
                .lastActivity(Instant.now())
                .build());

        Optional<Conversation> activeConv = conversationRepository.findByUserIdAndBusinessIdAndStatus(
                testCustomer.getId(), testBusiness.getId(), ConversationStatus.ACTIVE);
        assertThat(activeConv).isPresent();
        assertThat(activeConv.get().getId()).isEqualTo(conversation.getId());
    }
}
