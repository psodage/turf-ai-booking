package com.turfai.booking.repository;

import com.turfai.booking.entity.BlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, UUID> {

    List<BlockedSlot> findByTurfIdAndDate(UUID turfId, LocalDate date);
}
