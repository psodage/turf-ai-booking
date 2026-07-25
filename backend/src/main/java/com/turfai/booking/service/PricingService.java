package com.turfai.booking.service;

import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import com.turfai.booking.exception.BaseException;
import com.turfai.booking.exception.ErrorCode;
import com.turfai.booking.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final OperatingHoursService operatingHoursService;

    public record PriceResult(BigDecimal amount, PricingType type) {}

    /**
     * Resolves price for a slot using PEAK -> WEEKEND -> BASE priority.
     */
    public PriceResult calculateSlotPrice(UUID turfId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<PricingRule> rules = pricingRuleRepository.findByTurfId(turfId);
        int dayOfWeekIndex = operatingHoursService.toDayOfWeekIndex(date);
        boolean isWeekend = (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY);

        // 1. Check PEAK rules
        Optional<PricingRule> peakRule = rules.stream()
                .filter(r -> r.getPricingType() == PricingType.PEAK)
                .filter(r -> r.getDayOfWeek() == null || r.getDayOfWeek() == dayOfWeekIndex)
                .filter(r -> r.getStartTime() != null && r.getEndTime() != null)
                .filter(r -> !startTime.isBefore(r.getStartTime()) && !endTime.isAfter(r.getEndTime()))
                .findFirst();

        if (peakRule.isPresent()) {
            return new PriceResult(peakRule.get().getAmount(), PricingType.PEAK);
        }

        // 2. Check WEEKEND rules
        if (isWeekend) {
            Optional<PricingRule> weekendRule = rules.stream()
                    .filter(r -> r.getPricingType() == PricingType.WEEKEND)
                    .findFirst();
            if (weekendRule.isPresent()) {
                return new PriceResult(weekendRule.get().getAmount(), PricingType.WEEKEND);
            }
        }

        // 3. Fallback to BASE rule
        Optional<PricingRule> baseRule = rules.stream()
                .filter(r -> r.getPricingType() == PricingType.BASE)
                .findFirst();

        if (baseRule.isPresent()) {
            return new PriceResult(baseRule.get().getAmount(), PricingType.BASE);
        }

        throw new BaseException(ErrorCode.RESOURCE_NOT_FOUND, "No pricing rules configured for turf ID: " + turfId) {};
    }
}
