package com.turfai.booking.dto.report;

import com.turfai.booking.entity.Booking;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.Payment;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportData {

    private Business business;
    private ReportType reportType;
    private LocalDate startDate;
    private LocalDate endDate;

    private List<Booking> bookings;
    private List<Payment> payments;
    private List<CustomerSummaryDto> customerSummaries;
    private List<DailyRevenueDto> dailyRevenues;
    private List<SlotUtilizationDto> slotUtilizations;

    private BusinessSummaryDto summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BusinessSummaryDto {
        private long totalBookings;
        private long confirmedBookings;
        private long cancelledBookings;
        private long noShows;
        private BigDecimal totalRevenue;
        private BigDecimal totalRefunds;
        private BigDecimal averageBookingValue;
        private String peakBookingDay;
        private String peakBookingHour;
        private double occupancyPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerSummaryDto {
        private String name;
        private String phone;
        private long totalBookings;
        private BigDecimal totalSpend;
        private LocalDate firstBooking;
        private LocalDate lastBooking;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyRevenueDto {
        private LocalDate date;
        private long bookingCount;
        private BigDecimal revenue;
        private BigDecimal refunds;
        private BigDecimal netRevenue;
        private BigDecimal averageValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotUtilizationDto {
        private LocalDate date;
        private String turfName;
        private int availableSlots;
        private int bookedSlots;
        private double utilizationPercentage;
    }
}
