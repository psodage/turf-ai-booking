package com.turfai.booking.service;

import com.turfai.booking.dto.report.GenerateReportRequest;
import com.turfai.booking.dto.report.ReportResponse;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Report;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.repository.PaymentRepository;
import com.turfai.booking.repository.ReportRepository;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.repository.UserRepository;
import com.turfai.booking.service.report.ExcelGeneratorService;
import com.turfai.booking.service.report.ReportStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private ExcelGeneratorService excelGeneratorService;
    @Mock private ReportStorageService reportStorageService;

    private ReportService reportService;

    private Business testBusiness;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                businessRepository,
                bookingRepository,
                paymentRepository,
                turfRepository,
                userRepository,
                reportRepository,
                excelGeneratorService,
                reportStorageService
        );

        testBusiness = Business.builder().name("Green Pitch").build();
        testBusiness.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should generate report and save metadata")
    void testGenerateReportSuccess() {
        GenerateReportRequest req = GenerateReportRequest.builder()
                .businessId(testBusiness.getId())
                .reportType(ReportType.DAILY)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();

        when(businessRepository.findById(testBusiness.getId())).thenReturn(Optional.of(testBusiness));
        when(bookingRepository.findByBusinessIdAndBookingDateBetween(any(), any(), any())).thenReturn(List.of());
        when(paymentRepository.findByBusinessId(any())).thenReturn(List.of());
        when(turfRepository.findByBusinessId(any())).thenReturn(List.of());
        when(excelGeneratorService.generateExcelReport(any())).thenReturn(new byte[]{1, 2, 3});

        Report savedReport = Report.builder()
                .business(testBusiness)
                .reportType(ReportType.DAILY)
                .filePath("reports/test.xlsx")
                .build();
        savedReport.setId(UUID.randomUUID());

        when(reportStorageService.saveReport(any(), any(), any(), any(), any(), any())).thenReturn(savedReport);

        ReportResponse response = reportService.generateReport(req);

        assertThat(response).isNotNull();
        assertThat(response.getBusinessId()).isEqualTo(testBusiness.getId());
        verify(reportStorageService).saveReport(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw access denied when accessing report of another business")
    void testTenantIsolationSecurity() {
        UUID otherBusinessId = UUID.randomUUID();

        Report report = Report.builder()
                .business(testBusiness)
                .reportType(ReportType.DAILY)
                .filePath("reports/test.xlsx")
                .build();
        report.setId(UUID.randomUUID());

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.getReportById(report.getId(), otherBusinessId))
                .isInstanceOf(BaseException.class);
    }
}
