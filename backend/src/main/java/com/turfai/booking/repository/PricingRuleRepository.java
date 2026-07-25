package com.turfai.booking.repository;

import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {

    List<PricingRule> findByTurfId(UUID turfId);

    List<PricingRule> findByTurfIdAndPricingType(UUID turfId, PricingType pricingType);
}
