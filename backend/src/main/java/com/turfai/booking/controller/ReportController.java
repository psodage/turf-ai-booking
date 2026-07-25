package com.turfai.booking.controller;

import com.turfai.booking.dto.report.GenerateReportRequest;
import com.turfai.booking.dto.report.ReportResponse;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report Management API", description = "Endpoints for business report generation, downloading, and metadata lookup.")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "List Business Reports")
    public ResponseEntity<List<ReportResponse>> getReports(@RequestParam UUID businessId) {
        List<ReportResponse> reports = reportService.getReportsByBusiness(businessId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Report Metadata")
    public ResponseEntity<ReportResponse> getReport(@PathVariable UUID id, @RequestParam UUID businessId) {
        ReportResponse response = reportService.getReportById(id, businessId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download Excel Report File")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id, @RequestParam UUID businessId) {
        ReportResponse reportMeta = reportService.getReportById(id, businessId);
        byte[] excelBytes = reportService.downloadReportFile(id, businessId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportMeta.getFileName() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @PostMapping("/daily")
    @Operation(summary = "Generate Daily Report")
    public ResponseEntity<ReportResponse> generateDailyReport(
            @RequestParam UUID businessId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate reportDate = date != null ? date : LocalDate.now();
        ReportResponse response = reportService.generateReport(GenerateReportRequest.builder()
                .businessId(businessId)
                .reportType(ReportType.DAILY)
                .startDate(reportDate)
                .endDate(reportDate)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/weekly")
    @Operation(summary = "Generate Weekly Report")
    public ResponseEntity<ReportResponse> generateWeeklyReport(@RequestParam UUID businessId) {
        LocalDate today = LocalDate.now();
        ReportResponse response = reportService.generateReport(GenerateReportRequest.builder()
                .businessId(businessId)
                .reportType(ReportType.WEEKLY)
                .startDate(today.minusDays(6))
                .endDate(today)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/monthly")
    @Operation(summary = "Generate Monthly Report")
    public ResponseEntity<ReportResponse> generateMonthlyReport(@RequestParam UUID businessId) {
        LocalDate today = LocalDate.now();
        ReportResponse response = reportService.generateReport(GenerateReportRequest.builder()
                .businessId(businessId)
                .reportType(ReportType.MONTHLY)
                .startDate(today.withDayOfMonth(1))
                .endDate(today)
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/custom")
    @Operation(summary = "Generate Custom Range Report")
    public ResponseEntity<ReportResponse> generateCustomReport(@Valid @RequestBody GenerateReportRequest request) {
        request.setReportType(ReportType.CUSTOM);
        ReportResponse response = reportService.generateReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Report")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id, @RequestParam UUID businessId) {
        reportService.deleteReport(id, businessId);
        return ResponseEntity.noContent().build();
    }
}
