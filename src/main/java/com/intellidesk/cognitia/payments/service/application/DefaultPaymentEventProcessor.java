package com.intellidesk.cognitia.payments.service.application;

import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.payments.models.entities.Payment;
import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;
import com.intellidesk.cognitia.payments.models.enums.PaymentVerification;
import com.intellidesk.cognitia.payments.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of PaymentEventProcessor.
 * Handles business logic after payment webhook is received.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPaymentEventProcessor implements PaymentEventProcessor {

    private final OrderRepository orderRepository;

    @Override
    public void processPayment(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Processing payment: id={}, orderId={}, eventType={}, status={}",
            payment.getId(), payment.getOrderId(), payment.getEventType(), payment.getStatus());
        
        String eventType = payment.getEventType();
        
        switch (eventType) {
            case "payment.captured" -> handlePaymentCaptured(payment);
            case "payment.authorized" -> handlePaymentAuthorized(payment);
            case "payment.failed" -> handlePaymentFailed(payment);
            case "refund.created" -> handleRefundCreated(payment);
            case "refund.processed" -> handleRefundProcessed(payment);
            default -> log.warn("[DefaultPaymentEventProcessor] Unhandled event type: {}", eventType);
        }
    }
    
    private void handlePaymentCaptured(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Payment captured for order: {}", payment.getOrderId());
        
        // Check if order was already verified via frontend
        orderRepository.findByOrderId(payment.getOrderId()).ifPresent(order -> {
            if (order.getVerification() == PaymentVerification.SUCCESS) {
                log.info("[DefaultPaymentEventProcessor] Order {} already verified via frontend", payment.getOrderId());
                return;
            }
            
            // Fallback: If frontend verification didn't happen, update here
            if (order.getVerification() == null || order.getVerification() == PaymentVerification.PENDING) {
                log.info("[DefaultPaymentEventProcessor] Order {} not verified by frontend, updating via webhook", 
                    payment.getOrderId());
                orderRepository.updateVerificationByOrderId(payment.getOrderId(), PaymentVerification.SUCCESS);
            }
        });
    }
    
    private void handlePaymentAuthorized(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Payment authorized for order: {}", payment.getOrderId());
        // Payment authorized but not yet captured
    }
    
    private void handlePaymentFailed(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Payment failed for order: {}", payment.getOrderId());
        
        orderRepository.findByOrderId(payment.getOrderId()).ifPresent(order -> {
            if (order.getVerification() != PaymentVerification.FAILED) {
                orderRepository.updateVerificationByOrderId(payment.getOrderId(), PaymentVerification.FAILED);
            }
        });
    }
    
    private void handleRefundCreated(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Refund created for order: {}", payment.getOrderId());
        // Refund initiated but not yet processed
    }
    
    private void handleRefundProcessed(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Refund processed for order: {}", payment.getOrderId());
        
        // Mark fulfillment as expired (if applicable)
        orderRepository.updateFulfillmentStatusByOrderId(
            payment.getOrderId(),
            FulfillmentStatus.EXPIRED
        );
    }
}
