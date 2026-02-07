package com.intellidesk.cognitia.payments.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.intellidesk.cognitia.payments.models.entities.Payment;
import com.intellidesk.cognitia.payments.models.enums.ProcessingStatus;

public interface PaymentsRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find payments that need to be retried.
     * Includes:
     * - FAILED payments that haven't exceeded max attempts
     * - PENDING payments older than the threshold (event never fired)
     * - PROCESSING payments that are stuck (older than timeout)
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE (p.processingStatus = :failedStatus AND p.attempts < p.maxAttempts)
           OR (p.processingStatus = :pendingStatus AND p.persistedAt < :pendingThreshold)
           OR (p.processingStatus = :processingStatus AND p.persistedAt < :processingTimeout)
        ORDER BY p.persistedAt ASC
        """)
    List<Payment> findPaymentsNeedingRetry(
        @Param("failedStatus") ProcessingStatus failedStatus,
        @Param("pendingStatus") ProcessingStatus pendingStatus,
        @Param("processingStatus") ProcessingStatus processingStatus,
        @Param("pendingThreshold") OffsetDateTime pendingThreshold,
        @Param("processingTimeout") OffsetDateTime processingTimeout
    );
    
    List<Payment> findByProcessingStatusAndAttemptsLessThan(
        ProcessingStatus status, 
        Integer maxAttempts
    );
    
    List<Payment> findByProcessingStatusAndPersistedAtBefore(
        ProcessingStatus status,
        OffsetDateTime threshold
    );
}
