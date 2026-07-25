package com.turfai.booking.service;

import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.repository.OperatingHoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatingHoursService {

    private final OperatingHoursRepository operatingHoursRepository;

    /**
     * Converts LocalDate to day_of_week integer (0=Monday ... 6=Sunday).
     */
    public int toDayOfWeekIndex(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow.getValue() - 1; // 1=Mon -> 0, 7=Sun -> 6
    }

    public Optional<OperatingHours> getOperatingHours(UUID turfId, LocalDate date) {
        int dayOfWeekIndex = toDayOfWeekIndex(date);
        return operatingHoursRepository.findByTurfIdAndDayOfWeek(turfId, dayOfWeekIndex);
    }

    /**
     * Validates whether a time slot falls within operating hours.
     */
    public boolean isWithinOperatingHours(UUID turfId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Optional<OperatingHours> opHoursOpt = getOperatingHours(turfId, date);
        if (opHoursOpt.isEmpty()) {
            return false;
        }

        OperatingHours opHours = opHoursOpt.get();
        if (Boolean.TRUE.equals(opHours.getIsClosed())) {
            return false;
        }

        LocalTime openTime = opHours.getOpeningTime();
        LocalTime closeTime = opHours.getClosingTime();

        if (openTime == null || closeTime == null) {
            return false;
        }

        return !startTime.isBefore(openTime) && !endTime.isAfter(closeTime) && startTime.isBefore(endTime);
    }
}
