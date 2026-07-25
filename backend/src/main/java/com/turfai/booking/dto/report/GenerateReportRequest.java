package com.turfai.booking.dto.report;

import com.turfai.booking.entity.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateReportRequest {

    @NotNull(message = "Business ID is required")
    private UUID businessId;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    private LocalDate startDate;
    private LocalDate endDate;
    private UUID generatedByUserId;
}
