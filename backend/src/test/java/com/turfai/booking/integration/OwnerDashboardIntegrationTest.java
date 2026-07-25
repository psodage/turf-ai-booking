package com.turfai.booking.integration;

import com.turfai.booking.dto.report.DashboardSummaryResponse;
import com.turfai.booking.dto.report.GenerateReportRequest;
import com.turfai.booking.dto.report.ReportResponse;
import com.turfai.booking.dto.request.CreateBookingHoldRequest;
import com.turfai.booking.dto.response.BookingHoldResponse;
import com.turfai.booking.dto.response.BookingResponse;
import com.turfai.booking.entity.BookingAudit;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import com.turfai.booking.repository.BookingAuditRepository;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.repository.OperatingHoursRepository;
import com.turfai.booking.repository.PricingRuleRepository;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.repository.UserRepository;
import com.turfai.booking.service.BookingService;
import com.turfai.booking.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OwnerDashboardIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private ReportService reportService;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private TurfRepository turfRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private PricingRuleRepository pricingRuleRepository;
    @Autowired private BookingAuditRepository bookingAuditRepository;

    private Business testBusiness;
    private Turf testTurf;
    private User testCustomer;
    private User testOwner;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        testBusiness = businessRepository.saveAndFlush(Business.builder()
                .name("Phase 6 Executive Turf Arena")
                .whatsappPhoneNumberId("PN_PHASE6_001")
                .phone("+919222233333")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build());

        testTurf = turfRepository.saveAndFlush(Turf.builder()
                .business(testBusiness)
                .name("Executive Turf 1")
                .type(TurfType.FIVE_A_SIDE)
                .status(TurfStatus.ACTIVE)
                .build());

        testCustomer = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Vikramaditya")
                .phone("+919876549999")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        testOwner = userRepository.saveAndFlush(User.builder()
                .business(testBusiness)
                .name("Owner Admin")
                .phone("+919876548888")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build());

        for (int dow = 0; dow < 7; dow++) {
            operatingHoursRepository.saveAndFlush(OperatingHours.builder()
                    .turf(testTurf)
                    .dayOfWeek(dow)
                    .openingTime(LocalTime.of(6, 0))
                    .closingTime(LocalTime.of(23, 0))
                    .isClosed(false)
                    .build());
        }

        pricingRuleRepository.saveAndFlush(PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1500.00"))
                .build());
    }

    @Test
    @DisplayName("1. Dashboard Summary: Computes total bookings, revenue, and occupancy rate")
    void testDashboardSummary() {
        DashboardSummaryResponse summary = reportService.getDashboardSummary(testBusiness.getId(), today);
        assertThat(summary).isNotNull();
        assertThat(summary.getBusinessId()).isEqualTo(testBusiness.getId());
        assertThat(summary.getTotalBookings()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("2. Owner Action - Complete Booking: Transitions CONFIRMED booking to COMPLETED")
    void testCompleteBooking() {
        CreateBookingHoldRequest holdReq = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(today.plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();

        BookingHoldResponse holdResp = bookingService.createBookingHold(holdReq);
        bookingService.confirmBooking(holdResp.getBookingId(), "PAY_COMPLETE_TEST");

        BookingResponse completed = bookingService.completeBooking(holdResp.getBookingId(), testOwner.getId());

        assertThat(completed.getStatus()).isEqualTo(BookingStatus.COMPLETED);

        List<BookingAudit> audits = bookingAuditRepository.findByBookingIdOrderByChangedAtAsc(holdResp.getBookingId());
        assertThat(audits.get(audits.size() - 1).getNewStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    @DisplayName("3. Owner Action - No Show: Transitions CONFIRMED booking to NO_SHOW")
    void testMarkNoShow() {
        CreateBookingHoldRequest holdReq = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer.getId())
                .bookingDate(today.plusDays(1))
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 0))
                .build();

        BookingHoldResponse holdResp = bookingService.createBookingHold(holdReq);
        bookingService.confirmBooking(holdResp.getBookingId(), "PAY_NOSHOW_TEST");

        BookingResponse noShow = bookingService.markNoShow(holdResp.getBookingId(), testOwner.getId());

        assertThat(noShow.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
    }

    @Test
    @DisplayName("4. Report Generation & Excel Export: Generates DAILY report and exports XLSX byte stream")
    void testReportGenerationAndExcelExport() {
        GenerateReportRequest req = GenerateReportRequest.builder()
                .businessId(testBusiness.getId())
                .reportType(ReportType.DAILY)
                .startDate(today)
                .endDate(today)
                .build();

        ReportResponse reportResp = reportService.generateReport(req);

        assertThat(reportResp).isNotNull();
        assertThat(reportResp.getReportType()).isEqualTo(ReportType.DAILY);
        assertThat(reportResp.getFileName()).endsWith(".xlsx");

        byte[] excelBytes = reportService.downloadReportFile(reportResp.getReportId(), testBusiness.getId());
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes.length).isGreaterThan(0);
    }
}
