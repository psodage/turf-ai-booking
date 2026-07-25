package com.turfai.booking.repository;

import com.turfai.booking.entity.Report;
import com.turfai.booking.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByBusinessId(UUID businessId);

    List<Report> findByBusinessIdAndReportType(UUID businessId, ReportType reportType);
}
