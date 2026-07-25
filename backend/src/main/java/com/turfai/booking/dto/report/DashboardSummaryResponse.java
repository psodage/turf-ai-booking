package com.turfai.booking.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private UUID businessId;
    private LocalDate date;
    private int totalBookings;
    private int confirmedBookings;
    private int cancelledBookings;
    private BigDecimal totalRevenue;
    private double occupancyPercentage;
}
