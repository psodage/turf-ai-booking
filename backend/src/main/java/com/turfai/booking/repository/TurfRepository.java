package com.turfai.booking.repository;

import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TurfRepository extends JpaRepository<Turf, UUID> {

    List<Turf> findByBusinessId(UUID businessId);

    List<Turf> findByBusinessIdAndStatus(UUID businessId, TurfStatus status);
}
