package com.turfai.booking.dto.response;

import com.turfai.booking.entity.PricingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse {

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
    private BigDecimal price;
    private PricingType pricingType;
    private String unavailableReason;
}
