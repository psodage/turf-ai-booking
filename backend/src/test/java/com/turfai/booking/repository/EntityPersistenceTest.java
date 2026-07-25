package com.turfai.booking.repository;

import com.turfai.booking.entity.BlockReason;
import com.turfai.booking.entity.BlockedSlot;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingAudit;
import com.turfai.booking.entity.BookingHold;
import com.turfai.booking.entity.BookingSource;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationMessage;
import com.turfai.booking.entity.ConversationStatus;
import com.turfai.booking.entity.HoldStatus;
import com.turfai.booking.entity.MessageSender;
import com.turfai.booking.entity.MessageType;
import com.turfai.booking.entity.Notification;
import com.turfai.booking.entity.NotificationChannel;
import com.turfai.booking.entity.NotificationStatus;
import com.turfai.booking.entity.NotificationType;
import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.entity.Payment;
import com.turfai.booking.entity.PaymentAudit;
import com.turfai.booking.entity.PaymentGateway;
import com.turfai.booking.entity.PaymentStatus;
import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import com.turfai.booking.entity.RefundStatus;
import com.turfai.booking.entity.Report;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.entity.SystemSetting;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityPersistenceTest {

    @Autowired private BusinessRepository businessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TurfRepository turfRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private PricingRuleRepository pricingRuleRepository;
    @Autowired private BlockedSlotRepository blockedSlotRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingHoldRepository bookingHoldRepository;
    @Autowired private BookingAuditRepository bookingAuditRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAuditRepository paymentAuditRepository;
    @Autowired private SystemSettingRepository systemSettingRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationMessageRepository conversationMessageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ReportRepository reportRepository;

    @Test
    @DisplayName("Should persist and retrieve complete domain entity hierarchy with UUIDs and audit timestamps")
    void testCompleteEntityLifecycleAndRelationships() {
        // 1. Business
        Business business = Business.builder()
                .name("Apex Turf Arena")
                .address("100 Sports Way")
                .city("Pune")
                .state("Maharashtra")
                .pincode("411001")
                .phone("+919999988888")
                .whatsappPhoneNumberId("WA_APEX_001")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build();
        Business savedBusiness = businessRepository.saveAndFlush(business);
        assertThat(savedBusiness.getId()).isNotNull();
        assertThat(savedBusiness.getCreatedAt()).isNotNull();

        // 2. User
        User customer = User.builder()
                .business(savedBusiness)
                .name("Amit Kumar")
                .phone("+919876500000")
                .email("amit@example.com")
                .role(UserRole.CUSTOMER)
                .language("en")
                .status(UserStatus.ACTIVE)
                .build();
        User savedCustomer = userRepository.saveAndFlush(customer);
        assertThat(savedCustomer.getId()).isNotNull();

        // 3. Turf
        Turf turf = Turf.builder()
                .business(savedBusiness)
                .name("Turf Alpha")
                .type(TurfType.SEVEN_A_SIDE)
                .capacity(14)
                .status(TurfStatus.ACTIVE)
                .build();
        Turf savedTurf = turfRepository.saveAndFlush(turf);
        assertThat(savedTurf.getId()).isNotNull();

        // 4. OperatingHours
        OperatingHours opHours = OperatingHours.builder()
                .turf(savedTurf)
                .dayOfWeek(1)
                .openingTime(LocalTime.of(7, 0))
                .closingTime(LocalTime.of(22, 0))
                .isClosed(false)
                .build();
        OperatingHours savedOpHours = operatingHoursRepository.saveAndFlush(opHours);
        assertThat(savedOpHours.getId()).isNotNull();

        // 5. PricingRule
        PricingRule pricingRule = PricingRule.builder()
                .turf(savedTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1200.00"))
                .build();
        PricingRule savedPricingRule = pricingRuleRepository.saveAndFlush(pricingRule);
        assertThat(savedPricingRule.getId()).isNotNull();

        // 6. BlockedSlot
        BlockedSlot blockedSlot = BlockedSlot.builder()
                .turf(savedTurf)
                .date(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(14, 0))
                .reason(BlockReason.MAINTENANCE)
                .createdBy(savedCustomer)
                .build();
        BlockedSlot savedBlockedSlot = blockedSlotRepository.saveAndFlush(blockedSlot);
        assertThat(savedBlockedSlot.getId()).isNotNull();

        // 7. Booking
        Long seqVal = bookingRepository.getNextBookingSequenceValue();
        String bookingNumber = String.format("BK-%s-%06d", LocalDate.now().toString().replace("-", ""), seqVal);
        Booking booking = Booking.builder()
                .bookingNumber(bookingNumber)
                .business(savedBusiness)
                .turf(savedTurf)
                .customer(savedCustomer)
                .bookingDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .price(new BigDecimal("1200.00"))
                .status(BookingStatus.HOLD)
                .bookingSource(BookingSource.WHATSAPP_AI)
                .build();
        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        assertThat(savedBooking.getId()).isNotNull();

        // 8. BookingHold
        BookingHold hold = BookingHold.builder()
                .booking(savedBooking)
                .status(HoldStatus.ACTIVE)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        BookingHold savedHold = bookingHoldRepository.saveAndFlush(hold);
        assertThat(savedHold.getId()).isNotNull();

        // 9. BookingAudit
        BookingAudit bookingAudit = BookingAudit.builder()
                .booking(savedBooking)
                .oldStatus(null)
                .newStatus(BookingStatus.HOLD)
                .changedBy(savedCustomer)
                .reason("Initial hold creation")
                .build();
        BookingAudit savedBookingAudit = bookingAuditRepository.saveAndFlush(bookingAudit);
        assertThat(savedBookingAudit.getId()).isNotNull();

        // 10. Payment
        Payment payment = Payment.builder()
                .booking(savedBooking)
                .business(savedBusiness)
                .customer(savedCustomer)
                .amount(new BigDecimal("1200.00"))
                .currency("INR")
                .gateway(PaymentGateway.RAZORPAY)
                .gatewayOrderId("order_test_123")
                .status(PaymentStatus.CREATED)
                .refundStatus(RefundStatus.NOT_REQUIRED)
                .build();
        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        assertThat(savedPayment.getId()).isNotNull();

        // 11. PaymentAudit
        PaymentAudit paymentAudit = PaymentAudit.builder()
                .payment(savedPayment)
                .event("payment.created")
                .gatewayPayload("{\"order_id\":\"order_test_123\"}")
                .build();
        PaymentAudit savedPaymentAudit = paymentAuditRepository.saveAndFlush(paymentAudit);
        assertThat(savedPaymentAudit.getId()).isNotNull();

        // 12. SystemSetting
        SystemSetting setting = SystemSetting.builder()
                .settingKey("TEST_PERSISTENCE_SETTING_KEY")
                .value("100")
                .description("Test Description")
                .build();
        SystemSetting savedSetting = systemSettingRepository.saveAndFlush(setting);
        assertThat(savedSetting.getSettingKey()).isEqualTo("TEST_PERSISTENCE_SETTING_KEY");

        // 13. Conversation
        Conversation conversation = Conversation.builder()
                .user(savedCustomer)
                .business(savedBusiness)
                .role(UserRole.CUSTOMER)
                .currentIntent("BOOK_SLOT")
                .status(ConversationStatus.ACTIVE)
                .lastActivity(Instant.now())
                .build();
        Conversation savedConversation = conversationRepository.saveAndFlush(conversation);
        assertThat(savedConversation.getId()).isNotNull();

        // 14. ConversationMessage
        ConversationMessage message = ConversationMessage.builder()
                .conversation(savedConversation)
                .sender(MessageSender.USER)
                .message("I want to book a slot for tomorrow")
                .messageType(MessageType.TEXT)
                .whatsappMessageId("wamid.HBgLMTIzNDU2Nzg5")
                .build();
        ConversationMessage savedMessage = conversationMessageRepository.saveAndFlush(message);
        assertThat(savedMessage.getId()).isNotNull();

        // 15. Notification
        Notification notification = Notification.builder()
                .user(savedCustomer)
                .booking(savedBooking)
                .business(savedBusiness)
                .type(NotificationType.BOOKING_CONFIRMED)
                .channel(NotificationChannel.WHATSAPP)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
        Notification savedNotification = notificationRepository.saveAndFlush(notification);
        assertThat(savedNotification.getId()).isNotNull();

        // 16. Report
        Report report = Report.builder()
                .business(savedBusiness)
                .reportType(ReportType.DAILY)
                .filePath("reports/daily-rev.xlsx")
                .generatedBy(savedCustomer)
                .build();
        Report savedReport = reportRepository.saveAndFlush(report);
        assertThat(savedReport.getId()).isNotNull();
    }
}
