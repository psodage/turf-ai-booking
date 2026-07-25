package com.turfai.booking.repository;

import com.turfai.booking.entity.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OperatingHoursRepository extends JpaRepository<OperatingHours, UUID> {

    List<OperatingHours> findByTurfId(UUID turfId);

    Optional<OperatingHours> findByTurfIdAndDayOfWeek(UUID turfId, Integer dayOfWeek);
}
