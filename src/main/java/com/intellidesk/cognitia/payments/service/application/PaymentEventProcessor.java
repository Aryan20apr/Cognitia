package com.intellidesk.cognitia.payments.service.application;

import com.intellidesk.cognitia.payments.models.entities.Payment;

/**
 * Processes payment events after they are received from the payment gateway.
 * Implementations should contain business logic like:
 * - Updating order status
 * - Activating subscriptions/plans
 * - Sending notifications
 * - Updating fulfillment status
 */
public interface PaymentEventProcessor {
    
    /**
     * Process a payment entity.
     * Called asynchronously after the payment webhook is persisted.
     * 
     * @param payment The persisted payment record
     * @throws RuntimeException if processing fails (will be retried)
     */
    void processPayment(Payment payment);
}
