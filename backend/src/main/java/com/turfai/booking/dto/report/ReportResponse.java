package com.turfai.booking.dto.report;

import com.turfai.booking.entity.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private UUID reportId;
    private UUID businessId;
    private ReportType reportType;
    private String fileName;
    private String filePath;
    private String downloadUrl;
    private long fileSize;
    private Instant generatedAt;
    private UUID generatedByUserId;
}
