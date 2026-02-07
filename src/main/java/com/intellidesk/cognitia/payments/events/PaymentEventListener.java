package com.intellidesk.cognitia.payments.events;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.intellidesk.cognitia.payments.models.entities.Payment;
import com.intellidesk.cognitia.payments.models.enums.ProcessingStatus;
import com.intellidesk.cognitia.payments.repository.PaymentsRepository;
import com.intellidesk.cognitia.payments.service.application.PaymentEventProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listens for PaymentReceivedEvent and processes payments asynchronously.
 * Uses @TransactionalEventListener to ensure the event is only processed
 * after the original transaction (that saved the Payment) commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {
    
    private final PaymentsRepository paymentRepository;
    private final PaymentEventProcessor paymentEventProcessor;
    
    /**
     * Processes a payment after the transaction that created it commits.
     * Runs asynchronously to not block the webhook response.
     *
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        log.info("[PaymentEventListener] Processing payment {} for event {}", 
            event.getPaymentId(), event.getEventId());
        
        Optional<Payment> optionalPayment = paymentRepository.findById(event.getPaymentId());
        if (optionalPayment.isEmpty()) {
            log.error("[PaymentEventListener] Payment {} not found", event.getPaymentId());
            return;
        }
        
        Payment payment = optionalPayment.get();
        
        // Check if already processed (race condition protection)
        if (payment.getProcessingStatus() == ProcessingStatus.COMPLETED) {
            log.info("[PaymentEventListener] Payment {} already completed, skipping", payment.getId());
            return;
        }
        
        // Check if currently being processed by another instance
        if (payment.getProcessingStatus() == ProcessingStatus.PROCESSING) {
            log.info("[PaymentEventListener] Payment {} already processing, skipping", payment.getId());
            return;
        }
        
        processPayment(payment);
    }
    
    /**
     * Core processing logic - also used by the retry job.
     */
    @Transactional
    public void processPayment(Payment payment) {
        log.info("[PaymentEventListener] Starting processing for payment {}", payment.getId());
        
        // Mark as processing
        payment.setProcessingStatus(ProcessingStatus.PROCESSING);
        payment.setAttempts(payment.getAttempts() + 1);
        paymentRepository.save(payment);
        
        try {
            // Delegate to the processor for business logic
            paymentEventProcessor.processPayment(payment);
            
            // Mark as completed
            payment.setProcessingStatus(ProcessingStatus.COMPLETED);
            payment.setProcessedAt(OffsetDateTime.now());
            payment.setLastError(null);
            paymentRepository.save(payment);
            
            log.info("[PaymentEventListener] Successfully processed payment {}", payment.getId());
            
        } catch (Exception e) {
            log.error("[PaymentEventListener] Failed to process payment {}: {}", 
                payment.getId(), e.getMessage(), e);
            
            // Mark as failed for retry
            payment.setProcessingStatus(ProcessingStatus.FAILED);
            payment.setLastError(truncateError(e.getMessage()));
            paymentRepository.save(payment);
        }
    }
    
    private String truncateError(String error) {
        if (error == null) return null;
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }
}
