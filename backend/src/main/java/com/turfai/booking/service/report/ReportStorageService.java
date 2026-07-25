package com.turfai.booking.service.report;

import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Report;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.entity.User;
import com.turfai.booking.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportStorageService {

    private static final String STORAGE_DIR = "reports";
    private final ReportRepository reportRepository;

    @Transactional
    public Report saveReport(Business business, ReportType reportType, LocalDate startDate, LocalDate endDate, User generatedBy, byte[] content) {
        try {
            Path dirPath = Paths.get(STORAGE_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String fileName = generateFileName(reportType, startDate, endDate);
            File targetFile = dirPath.resolve(fileName).toFile();

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(content);
            }

            Report report = Report.builder()
                    .business(business)
                    .reportType(reportType)
                    .filePath(targetFile.getAbsolutePath())
                    .generatedBy(generatedBy)
                    .build();

            report = reportRepository.save(report);
            log.info("Saved report metadata and file: ID={}, path={}", report.getId(), targetFile.getAbsolutePath());
            return report;

        } catch (IOException ex) {
            log.error("Failed to store report file", ex);
            throw new RuntimeException("Report file storage error: " + ex.getMessage(), ex);
        }
    }

    public byte[] loadReportFile(Report report) {
        try {
            Path filePath = Paths.get(report.getFilePath());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Report file not found on disk at: " + report.getFilePath());
            }
            return Files.readAllBytes(filePath);
        } catch (IOException ex) {
            log.error("Failed to read report file {}", report.getFilePath(), ex);
            throw new RuntimeException("Report file read error: " + ex.getMessage(), ex);
        }
    }

    private String generateFileName(ReportType reportType, LocalDate startDate, LocalDate endDate) {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (reportType == ReportType.CUSTOM && startDate != null && endDate != null) {
            return String.format("Report_Custom_%s_to_%s_%s.xlsx", startDate, endDate, UUID.randomUUID().toString().substring(0, 6));
        }
        return String.format("Report_%s_%s_%s.xlsx", reportType.name(), timestamp, UUID.randomUUID().toString().substring(0, 6));
    }
}
