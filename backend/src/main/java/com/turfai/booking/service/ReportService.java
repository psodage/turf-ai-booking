package com.turfai.booking.service;

import com.turfai.booking.dto.report.GenerateReportRequest;
import com.turfai.booking.dto.report.ReportData;
import com.turfai.booking.dto.report.ReportResponse;
import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.BookingStatus;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Payment;
import com.turfai.booking.entity.PaymentStatus;
import com.turfai.booking.entity.RefundStatus;
import com.turfai.booking.entity.Report;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.User;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.exception.ErrorCode;
import com.turfai.booking.repository.BookingRepository;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.repository.PaymentRepository;
import com.turfai.booking.repository.ReportRepository;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.repository.UserRepository;
import com.turfai.booking.service.report.ExcelGeneratorService;
import com.turfai.booking.service.report.ReportStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BusinessRepository businessRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TurfRepository turfRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ExcelGeneratorService excelGeneratorService;
    private final ReportStorageService reportStorageService;

    @Transactional
    public ReportResponse generateReport(GenerateReportRequest request) {
        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Business not found with ID: " + request.getBusinessId()) {});

        User generatedBy = null;
        if (request.getGeneratedByUserId() != null) {
            generatedBy = userRepository.findById(request.getGeneratedByUserId()).orElse(null);
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            switch (request.getReportType()) {
                case DAILY -> {
                    startDate = today;
                    endDate = today;
                }
                case WEEKLY -> {
                    startDate = today.minusDays(6);
                    endDate = today;
                }
                case MONTHLY -> {
                    startDate = today.withDayOfMonth(1);
                    endDate = today;
                }
                case CUSTOM -> {
                    startDate = today.minusDays(30);
                    endDate = today;
                }
            }
        }

        log.info("Generating {} report for business {} ({}) from {} to {}",
                request.getReportType(), business.getName(), business.getId(), startDate, endDate);

        // Fetch bookings & payments in date range
        List<Booking> bookings = bookingRepository.findByBusinessIdAndBookingDateBetween(business.getId(), startDate, endDate);
        List<Payment> payments = paymentRepository.findByBusinessId(business.getId()).stream()
                .filter(p -> {
                    LocalDate pDate = p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return !pDate.isBefore(startDate) && !pDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        List<Turf> turfs = turfRepository.findByBusinessId(business.getId());

        // Aggregate ReportData
        ReportData reportData = buildReportData(business, request.getReportType(), startDate, endDate, bookings, payments, turfs);

        // Generate Excel Bytes
        byte[] excelBytes = excelGeneratorService.generateExcelReport(reportData);

        // Save file & metadata
        Report report = reportStorageService.saveReport(business, request.getReportType(), startDate, endDate, generatedBy, excelBytes);

        return mapToReportResponse(report, excelBytes.length);
    }

    @Transactional(readOnly = true)
    public ReportResponse getReportById(UUID reportId, UUID businessId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found with ID: " + reportId) {});

        if (!report.getBusiness().getId().equals(businessId)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED_BUSINESS_ACCESS, "Access denied: Report belongs to another business.") {};
        }

        File file = new File(report.getFilePath());
        return mapToReportResponse(report, file.exists() ? file.length() : 0);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByBusiness(UUID businessId) {
        return reportRepository.findByBusinessId(businessId).stream()
                .map(r -> {
                    File file = new File(r.getFilePath());
                    return mapToReportResponse(r, file.exists() ? file.length() : 0);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] downloadReportFile(UUID reportId, UUID businessId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found with ID: " + reportId) {});

        if (!report.getBusiness().getId().equals(businessId)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED_BUSINESS_ACCESS, "Access denied: Report belongs to another business.") {};
        }

        return reportStorageService.loadReportFile(report);
    }

    @Transactional
    public void deleteReport(UUID reportId, UUID businessId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found with ID: " + reportId) {});

        if (!report.getBusiness().getId().equals(businessId)) {
            throw new BaseException(ErrorCode.UNAUTHORIZED_BUSINESS_ACCESS, "Access denied: Report belongs to another business.") {};
        }

        File file = new File(report.getFilePath());
        if (file.exists()) {
            file.delete();
        }
        reportRepository.delete(report);
    }

    private ReportData buildReportData(Business business, ReportType reportType, LocalDate startDate, LocalDate endDate,
                                      List<Booking> bookings, List<Payment> payments, List<Turf> turfs) {

        long totalBookings = bookings.size();
        long confirmed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long cancelled = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long noShows = bookings.stream().filter(b -> b.getStatus() == BookingStatus.NO_SHOW).count();

        BigDecimal totalRevenue = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                .map(Booking::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRefunds = payments.stream()
                .filter(p -> p.getRefundStatus() == RefundStatus.SUCCESS)
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgBookingValue = confirmed > 0
                ? totalRevenue.divide(BigDecimal.valueOf(confirmed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String peakDay = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getBookingDate().getDayOfWeek().name(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String peakHour = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStartTime().toString(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        long totalDays = startDate.until(endDate.plusDays(1)).getDays();
        long totalPossibleSlots = (turfs != null ? turfs.size() : 1) * totalDays * 16; // 16 hours open/day assumption
        double occupancyPct = totalPossibleSlots > 0 ? ((double) confirmed / totalPossibleSlots) * 100.0 : 0.0;

        ReportData.BusinessSummaryDto summary = ReportData.BusinessSummaryDto.builder()
                .totalBookings(totalBookings)
                .confirmedBookings(confirmed)
                .cancelledBookings(cancelled)
                .noShows(noShows)
                .totalRevenue(totalRevenue)
                .totalRefunds(totalRefunds)
                .averageBookingValue(avgBookingValue)
                .peakBookingDay(peakDay)
                .peakBookingHour(peakHour)
                .occupancyPercentage(Math.min(100.0, occupancyPct))
                .build();

        // Build Customers summary
        Map<User, List<Booking>> customerMap = bookings.stream().collect(Collectors.groupingBy(Booking::getCustomer));
        List<ReportData.CustomerSummaryDto> customerSummaries = customerMap.entrySet().stream()
                .map(e -> {
                    User customer = e.getKey();
                    List<Booking> cBookings = e.getValue();
                    BigDecimal spend = cBookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                            .map(Booking::getPrice)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    LocalDate firstB = cBookings.stream().map(Booking::getBookingDate).min(LocalDate::compareTo).orElse(startDate);
                    LocalDate lastB = cBookings.stream().map(Booking::getBookingDate).max(LocalDate::compareTo).orElse(endDate);

                    return ReportData.CustomerSummaryDto.builder()
                            .name(customer.getName())
                            .phone(customer.getPhone())
                            .totalBookings(cBookings.size())
                            .totalSpend(spend)
                            .firstBooking(firstB)
                            .lastBooking(lastB)
                            .build();
                })
                .sorted(Comparator.comparing(ReportData.CustomerSummaryDto::getTotalSpend).reversed())
                .collect(Collectors.toList());

        // Build Daily Revenues
        Map<LocalDate, List<Booking>> dailyMap = bookings.stream().collect(Collectors.groupingBy(Booking::getBookingDate));
        List<ReportData.DailyRevenueDto> dailyRevenues = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            List<Booking> dayBookings = dailyMap.getOrDefault(d, List.of());
            long dayConfirmed = dayBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED).count();
            BigDecimal dayRev = dayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
                    .map(Booking::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal dayRef = payments.stream()
                    .filter(p -> p.getRefundStatus() == RefundStatus.SUCCESS && p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().equals(d))
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netRev = dayRev.subtract(dayRef);
            BigDecimal dayAvg = dayConfirmed > 0 ? dayRev.divide(BigDecimal.valueOf(dayConfirmed), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            dailyRevenues.add(ReportData.DailyRevenueDto.builder()
                    .date(d)
                    .bookingCount(dayBookings.size())
                    .revenue(dayRev)
                    .refunds(dayRef)
                    .netRevenue(netRev)
                    .averageValue(dayAvg)
                    .build());
        }

        // Build Slot Utilizations
        List<ReportData.SlotUtilizationDto> slotUtilizations = new ArrayList<>();
        if (turfs != null) {
            for (Turf t : turfs) {
                for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                    final LocalDate dateRef = d;
                    long bookedSlots = bookings.stream()
                            .filter(b -> b.getTurf().getId().equals(t.getId()) && b.getBookingDate().equals(dateRef) && (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED))
                            .count();
                    int availSlots = 16;
                    double utilPct = ((double) bookedSlots / availSlots) * 100.0;

                    slotUtilizations.add(ReportData.SlotUtilizationDto.builder()
                            .date(d)
                            .turfName(t.getName())
                            .availableSlots(availSlots)
                            .bookedSlots((int) bookedSlots)
                            .utilizationPercentage(Math.min(100.0, utilPct))
                            .build());
                }
            }
        }

        return ReportData.builder()
                .business(business)
                .reportType(reportType)
                .startDate(startDate)
                .endDate(endDate)
                .bookings(bookings)
                .payments(payments)
                .customerSummaries(customerSummaries)
                .dailyRevenues(dailyRevenues)
                .slotUtilizations(slotUtilizations)
                .summary(summary)
                .build();
    }

    private ReportResponse mapToReportResponse(Report r, long fileSize) {
        File file = new File(r.getFilePath());
        return ReportResponse.builder()
                .reportId(r.getId())
                .businessId(r.getBusiness().getId())
                .reportType(r.getReportType())
                .fileName(file.getName())
                .filePath(r.getFilePath())
                .downloadUrl("/api/v1/reports/" + r.getId() + "/download?businessId=" + r.getBusiness().getId())
                .fileSize(fileSize)
                .generatedAt(r.getGeneratedAt())
                .generatedByUserId(r.getGeneratedBy() != null ? r.getGeneratedBy().getId() : null)
                .build();
    }
}
