package com.turfai.booking.controller;

import com.turfai.booking.dto.report.ReportResponse;
import com.turfai.booking.entity.ReportType;
import com.turfai.booking.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ReportService reportService;

    @Test
    @DisplayName("GET /api/v1/reports should list business reports")
    void testGetReportsList() throws Exception {
        UUID businessId = UUID.randomUUID();
        ReportResponse response = ReportResponse.builder()
                .reportId(UUID.randomUUID())
                .businessId(businessId)
                .reportType(ReportType.DAILY)
                .fileName("Daily_Report.xlsx")
                .build();

        when(reportService.getReportsByBusiness(businessId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reports").param("businessId", businessId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("Daily_Report.xlsx"));
    }

    @Test
    @DisplayName("POST /api/v1/reports/daily should generate daily report")
    void testGenerateDailyReport() throws Exception {
        UUID businessId = UUID.randomUUID();
        ReportResponse response = ReportResponse.builder()
                .reportId(UUID.randomUUID())
                .businessId(businessId)
                .reportType(ReportType.DAILY)
                .fileName("Daily_Report.xlsx")
                .build();

        when(reportService.generateReport(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reports/daily").param("businessId", businessId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportType").value("DAILY"));
    }
}
