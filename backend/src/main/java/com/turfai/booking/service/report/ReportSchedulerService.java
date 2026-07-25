package com.turfai.booking.service.report;

import com.turfai.booking.dto.report.GenerateReportRequest;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSchedulerService {

    private final BusinessRepository businessRepository;
    private final ReportService reportService;

    // Daily Report — Every night at 23:59
    @Scheduled(cron = "0 59 23 * * *")
    public void generateDailyReports() {
        log.info("Starting scheduled daily report generation...");
        List<Business> businesses = businessRepository.findAll();
        for (Business b : businesses) {
            try {
                reportService.generateReport(GenerateReportRequest.builder()
                        .businessId(b.getId())
                        .reportType(ReportType.DAILY)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .build());
            } catch (Exception ex) {
                log.error("Scheduled daily report failed for business {}", b.getName(), ex);
            }
        }
    }

    // Weekly Report — Every Monday at 08:00 AM
    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklyReports() {
        log.info("Starting scheduled weekly report generation...");
        LocalDate today = LocalDate.now();
        List<Business> businesses = businessRepository.findAll();
        for (Business b : businesses) {
            try {
                reportService.generateReport(GenerateReportRequest.builder()
                        .businessId(b.getId())
                        .reportType(ReportType.WEEKLY)
                        .startDate(today.minusDays(7))
                        .endDate(today.minusDays(1))
                        .build());
            } catch (Exception ex) {
                log.error("Scheduled weekly report failed for business {}", b.getName(), ex);
            }
        }
    }

    // Monthly Report — 1st day of every month at 08:00 AM
    @Scheduled(cron = "0 0 8 1 * *")
    public void generateMonthlyReports() {
        log.info("Starting scheduled monthly report generation...");
        LocalDate today = LocalDate.now();
        LocalDate firstOfPrevMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastOfPrevMonth = today.withDayOfMonth(1).minusDays(1);

        List<Business> businesses = businessRepository.findAll();
        for (Business b : businesses) {
            try {
                reportService.generateReport(GenerateReportRequest.builder()
                        .businessId(b.getId())
                        .reportType(ReportType.MONTHLY)
                        .startDate(firstOfPrevMonth)
                        .endDate(lastOfPrevMonth)
                        .build());
            } catch (Exception ex) {
                log.error("Scheduled monthly report failed for business {}", b.getName(), ex);
            }
        }
    }
}
