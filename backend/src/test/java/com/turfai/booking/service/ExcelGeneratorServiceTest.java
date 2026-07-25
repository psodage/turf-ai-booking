package com.turfai.booking.service;

import com.turfai.booking.dto.report.ReportData;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.service.report.ExcelGeneratorService;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelGeneratorServiceTest {

    private ExcelGeneratorService excelGeneratorService;

    @BeforeEach
    void setUp() {
        excelGeneratorService = new ExcelGeneratorService();
    }

    @Test
    @DisplayName("Should sanitize formula injection characters with single quote")
    void testFormulaInjectionSanitization() {
        assertThat(excelGeneratorService.sanitize("=SUM(A1:A5)")).isEqualTo("'=SUM(A1:A5)");
        assertThat(excelGeneratorService.sanitize("+cmd|' /C calc'!A0")).isEqualTo("'+cmd|' /C calc'!A0");
        assertThat(excelGeneratorService.sanitize("-10")).isEqualTo("'-10");
        assertThat(excelGeneratorService.sanitize("@SUM(1,2)")).isEqualTo("'@SUM(1,2)");
        assertThat(excelGeneratorService.sanitize("Normal Text")).isEqualTo("Normal Text");
    }

    @Test
    @DisplayName("Should generate valid Excel workbook with 6 sheets")
    void testGenerateExcelReport() throws Exception {
        Business business = Business.builder().name("Green Pitch").build();
        ReportData reportData = ReportData.builder()
                .business(business)
                .reportType(ReportType.DAILY)
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now())
                .bookings(java.util.List.of())
                .payments(java.util.List.of())
                .customerSummaries(java.util.List.of())
                .dailyRevenues(java.util.List.of())
                .slotUtilizations(java.util.List.of())
                .summary(ReportData.BusinessSummaryDto.builder()
                        .totalBookings(10)
                        .confirmedBookings(8)
                        .cancelledBookings(2)
                        .totalRevenue(java.math.BigDecimal.valueOf(8000))
                        .build())
                .build();

        byte[] bytes = excelGeneratorService.generateExcelReport(reportData);

        assertThat(bytes).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(6);
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("Business Summary");
            assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("Bookings");
            assertThat(workbook.getSheetAt(2).getSheetName()).isEqualTo("Payments");
            assertThat(workbook.getSheetAt(3).getSheetName()).isEqualTo("Customers");
            assertThat(workbook.getSheetAt(4).getSheetName()).isEqualTo("Revenue");
            assertThat(workbook.getSheetAt(5).getSheetName()).isEqualTo("Slot Utilization");
        }
    }
}
