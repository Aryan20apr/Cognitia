package com.intellidesk.cognitia.payments.scheduler;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.payments.events.PaymentEventListener;
import com.intellidesk.cognitia.payments.models.entities.Payment;
import com.intellidesk.cognitia.payments.models.enums.ProcessingStatus;
import com.intellidesk.cognitia.payments.repository.PaymentsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job to retry failed payment processing.
 * Acts as a fallback mechanism for:
 * - Failed payments that need retry
 * - Pending payments where the async event never fired (e.g., JVM crash)
 * - Stuck processing payments (timeout protection)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRetryScheduler {
    
    private final PaymentsRepository paymentRepository;
    private final PaymentEventListener paymentEventListener;
    
    // Thresholds for identifying stuck payments
    private static final int PENDING_THRESHOLD_SECONDS = 60;     
    private static final int PROCESSING_TIMEOUT_SECONDS = 300; 
    
    /**
     * Runs every 30 seconds to pick up failed/stuck payments.
     * Uses a short interval because payment processing should be timely.
     */
    @Scheduled(fixedDelay = 30000)  // 30 seconds
    public void retryFailedPayments() {
        log.debug("[PaymentRetryScheduler] Starting retry check");
        
        OffsetDateTime pendingThreshold = OffsetDateTime.now().minusSeconds(PENDING_THRESHOLD_SECONDS);
        OffsetDateTime processingTimeout = OffsetDateTime.now().minusSeconds(PROCESSING_TIMEOUT_SECONDS);
        
        List<Payment> paymentsToRetry = paymentRepository.findPaymentsNeedingRetry(
            ProcessingStatus.FAILED,
            ProcessingStatus.PENDING,
            ProcessingStatus.PROCESSING,
            pendingThreshold,
            processingTimeout
        );
        
        if (paymentsToRetry.isEmpty()) {
            log.debug("[PaymentRetryScheduler] No payments need retry");
            return;
        }
        
        log.info("[PaymentRetryScheduler] Found {} payments needing retry", paymentsToRetry.size());
        
        for (Payment payment : paymentsToRetry) {
            retryPayment(payment);
        }
    }
    
    private void retryPayment(Payment payment) {
        try {
            log.info("[PaymentRetryScheduler] Retrying payment {} (status: {}, attempts: {}/{})",
                payment.getId(), 
                payment.getProcessingStatus(),
                payment.getAttempts(),
                payment.getMaxAttempts());
            
            // Check if max attempts exceeded
            if (payment.getAttempts() >= payment.getMaxAttempts()) {
                log.warn("[PaymentRetryScheduler] Payment {} exceeded max attempts, marking as DEAD",
                    payment.getId());
                payment.setProcessingStatus(ProcessingStatus.FAILED);
                payment.setLastError("Exceeded maximum retry attempts");
                paymentRepository.save(payment);
                
                return;
            }
            
            
            paymentEventListener.processPayment(payment);
            
        } catch (Exception e) {
            log.error("[PaymentRetryScheduler] Error retrying payment {}: {}", 
                payment.getId(), e.getMessage(), e);
        }
    }
}
