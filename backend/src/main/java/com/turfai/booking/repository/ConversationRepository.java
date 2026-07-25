package com.turfai.booking.repository;

import com.turfai.booking.entity.Conversation;
import com.turfai.booking.entity.ConversationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserIdAndBusinessId(UUID userId, UUID businessId);

    Optional<Conversation> findByUserIdAndBusinessIdAndStatus(UUID userId, UUID businessId, ConversationStatus status);

    /**
     * ADR-018: Acquires pessimistic write lock (SELECT FOR UPDATE) on conversation row.
     * Ensures messages for the same conversation process sequentially.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Conversation> findWithLockByUserIdAndBusinessIdAndStatus(UUID userId, UUID businessId, ConversationStatus status);
}
