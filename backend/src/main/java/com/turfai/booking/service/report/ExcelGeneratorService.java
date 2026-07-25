package com.turfai.booking.service.report;

import com.turfai.booking.dto.report.ReportData;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExcelGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generateExcelReport(ReportData data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // 1. Business Summary
            buildSummarySheet(workbook, data, headerStyle, currencyStyle);

            // 2. Bookings
            buildBookingsSheet(workbook, data.getBookings(), headerStyle, currencyStyle);

            // 3. Payments
            buildPaymentsSheet(workbook, data.getPayments(), headerStyle, currencyStyle);

            // 4. Customers
            buildCustomersSheet(workbook, data.getCustomerSummaries(), headerStyle, currencyStyle);

            // 5. Revenue
            buildRevenueSheet(workbook, data.getDailyRevenues(), headerStyle, currencyStyle);

            // 6. Slot Utilization
            buildUtilizationSheet(workbook, data.getSlotUtilizations(), headerStyle);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException ex) {
            log.error("Failed to generate Excel report", ex);
            throw new RuntimeException("Excel report generation failed: " + ex.getMessage(), ex);
        }
    }

    private void buildSummarySheet(Workbook workbook, ReportData data, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Business Summary");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Metric", "Value"};
        createHeaderRow(sheet, headers, headerStyle);

        ReportData.BusinessSummaryDto summary = data.getSummary();
        int rowIdx = 1;

        createRow(sheet, rowIdx++, "Business Name", sanitize(data.getBusiness().getName()));
        createRow(sheet, rowIdx++, "Report Type", data.getReportType().name());
        createRow(sheet, rowIdx++, "Date Range", data.getStartDate() + " to " + data.getEndDate());
        createRow(sheet, rowIdx++, "Total Bookings", summary != null ? summary.getTotalBookings() : 0);
        createRow(sheet, rowIdx++, "Confirmed Bookings", summary != null ? summary.getConfirmedBookings() : 0);
        createRow(sheet, rowIdx++, "Cancelled Bookings", summary != null ? summary.getCancelledBookings() : 0);
        createRow(sheet, rowIdx++, "No Shows", summary != null ? summary.getNoShows() : 0);
        createCurrencyRow(sheet, rowIdx++, "Total Revenue", summary != null ? summary.getTotalRevenue() : BigDecimal.ZERO, currencyStyle);
        createCurrencyRow(sheet, rowIdx++, "Total Refunds", summary != null ? summary.getTotalRefunds() : BigDecimal.ZERO, currencyStyle);
        createCurrencyRow(sheet, rowIdx++, "Average Booking Value", summary != null ? summary.getAverageBookingValue() : BigDecimal.ZERO, currencyStyle);
        createRow(sheet, rowIdx++, "Peak Booking Day", summary != null ? sanitize(summary.getPeakBookingDay()) : "N/A");
        createRow(sheet, rowIdx++, "Peak Booking Hour", summary != null ? sanitize(summary.getPeakBookingHour()) : "N/A");
        createRow(sheet, rowIdx++, "Occupancy Percentage", String.format("%.2f%%", summary != null ? summary.getOccupancyPercentage() : 0.0));

        autoSizeColumns(sheet, headers.length);
    }

    private void buildBookingsSheet(Workbook workbook, List<Booking> bookings, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Bookings");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Booking Number", "Customer Name", "Phone", "Turf", "Date", "Start Time", "End Time", "Amount", "Status", "Source", "Created At"};
        createHeaderRow(sheet, headers, headerStyle);

        int rowIdx = 1;
        if (bookings != null && !bookings.isEmpty()) {
            for (Booking b : bookings) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sanitize(b.getBookingNumber()));
                row.createCell(1).setCellValue(sanitize(b.getCustomer().getName()));
                row.createCell(2).setCellValue(sanitize(b.getCustomer().getPhone()));
                row.createCell(3).setCellValue(sanitize(b.getTurf().getName()));
                row.createCell(4).setCellValue(b.getBookingDate().format(DATE_FORMATTER));
                row.createCell(5).setCellValue(b.getStartTime().format(TIME_FORMATTER));
                row.createCell(6).setCellValue(b.getEndTime().format(TIME_FORMATTER));

                Cell amountCell = row.createCell(7);
                amountCell.setCellValue(b.getPrice() != null ? b.getPrice().doubleValue() : 0.0);
                amountCell.setCellStyle(currencyStyle);

                row.createCell(8).setCellValue(b.getStatus().name());
                row.createCell(9).setCellValue(b.getBookingSource() != null ? b.getBookingSource().name() : "WHATSAPP");
                row.createCell(10).setCellValue(b.getCreatedAt() != null ? DATETIME_FORMATTER.format(b.getCreatedAt().atZone(java.time.ZoneId.systemDefault())) : "");
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void buildPaymentsSheet(Workbook workbook, List<Payment> payments, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Payments");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Booking Number", "Payment ID", "Gateway Payment ID", "Amount", "Status", "Refund Status", "Payment Time"};
        createHeaderRow(sheet, headers, headerStyle);

        int rowIdx = 1;
        if (payments != null && !payments.isEmpty()) {
            for (Payment p : payments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sanitize(p.getBooking().getBookingNumber()));
                row.createCell(1).setCellValue(p.getId().toString());
                row.createCell(2).setCellValue(sanitize(p.getGatewayPaymentId()));

                Cell amountCell = row.createCell(3);
                amountCell.setCellValue(p.getAmount() != null ? p.getAmount().doubleValue() : 0.0);
                amountCell.setCellStyle(currencyStyle);

                row.createCell(4).setCellValue(p.getStatus().name());
                row.createCell(5).setCellValue(p.getRefundStatus() != null ? p.getRefundStatus().name() : "NOT_REQUIRED");
                row.createCell(6).setCellValue(p.getCreatedAt() != null ? DATETIME_FORMATTER.format(p.getCreatedAt().atZone(java.time.ZoneId.systemDefault())) : "");
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void buildCustomersSheet(Workbook workbook, List<ReportData.CustomerSummaryDto> customers, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Customers");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Customer Name", "Phone", "Total Bookings", "Total Spend", "First Booking", "Last Booking"};
        createHeaderRow(sheet, headers, headerStyle);

        int rowIdx = 1;
        if (customers != null && !customers.isEmpty()) {
            for (ReportData.CustomerSummaryDto c : customers) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sanitize(c.getName()));
                row.createCell(1).setCellValue(sanitize(c.getPhone()));
                row.createCell(2).setCellValue(c.getTotalBookings());

                Cell spendCell = row.createCell(3);
                spendCell.setCellValue(c.getTotalSpend() != null ? c.getTotalSpend().doubleValue() : 0.0);
                spendCell.setCellStyle(currencyStyle);

                row.createCell(4).setCellValue(c.getFirstBooking() != null ? c.getFirstBooking().format(DATE_FORMATTER) : "");
                row.createCell(5).setCellValue(c.getLastBooking() != null ? c.getLastBooking().format(DATE_FORMATTER) : "");
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void buildRevenueSheet(Workbook workbook, List<ReportData.DailyRevenueDto> revenues, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Revenue");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Date", "Bookings", "Gross Revenue", "Refunds", "Net Revenue", "Avg Booking Value"};
        createHeaderRow(sheet, headers, headerStyle);

        int rowIdx = 1;
        if (revenues != null && !revenues.isEmpty()) {
            for (ReportData.DailyRevenueDto r : revenues) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getDate().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(r.getBookingCount());

                Cell revCell = row.createCell(2);
                revCell.setCellValue(r.getRevenue() != null ? r.getRevenue().doubleValue() : 0.0);
                revCell.setCellStyle(currencyStyle);

                Cell refCell = row.createCell(3);
                refCell.setCellValue(r.getRefunds() != null ? r.getRefunds().doubleValue() : 0.0);
                refCell.setCellStyle(currencyStyle);

                Cell netCell = row.createCell(4);
                netCell.setCellValue(r.getNetRevenue() != null ? r.getNetRevenue().doubleValue() : 0.0);
                netCell.setCellStyle(currencyStyle);

                Cell avgCell = row.createCell(5);
                avgCell.setCellValue(r.getAverageValue() != null ? r.getAverageValue().doubleValue() : 0.0);
                avgCell.setCellStyle(currencyStyle);
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    private void buildUtilizationSheet(Workbook workbook, List<ReportData.SlotUtilizationDto> utilizations, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Slot Utilization");
        sheet.createFreezePane(0, 1);

        String[] headers = {"Date", "Turf", "Available Slots", "Booked Slots", "Utilization %"};
        createHeaderRow(sheet, headers, headerStyle);

        int rowIdx = 1;
        if (utilizations != null && !utilizations.isEmpty()) {
            for (ReportData.SlotUtilizationDto u : utilizations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getDate().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(sanitize(u.getTurfName()));
                row.createCell(2).setCellValue(u.getAvailableSlots());
                row.createCell(3).setCellValue(u.getBookedSlots());
                row.createCell(4).setCellValue(String.format("%.2f%%", u.getUtilizationPercentage()));
            }
        }

        autoSizeColumns(sheet, headers.length);
    }

    // Formula Injection Security: Prefixes dangerous formula characters with single quote
    public String sanitize(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) {
            return "'" + trimmed;
        }
        return trimmed;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("₹#,##0.00"));
        return style;
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createRow(Sheet sheet, int rowIdx, String label, Object value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value.toString() : "");
    }

    private void createCurrencyRow(Sheet sheet, int rowIdx, String label, BigDecimal value, CellStyle currencyStyle) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        Cell cell = row.createCell(1);
        cell.setCellValue(value != null ? value.doubleValue() : 0.0);
        cell.setCellStyle(currencyStyle);
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
