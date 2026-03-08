package com.intellidesk.cognitia.payments.service.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.AssignPlanRequest;
import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.payments.models.entities.Payment;
import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;
import com.intellidesk.cognitia.payments.models.enums.OrderStatus;
import com.intellidesk.cognitia.payments.models.enums.PaymentPurpose;
import com.intellidesk.cognitia.payments.models.enums.PaymentVerification;
import com.intellidesk.cognitia.payments.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPaymentEventProcessor implements PaymentEventProcessor {

    private final OrderRepository orderRepository;
    private final QuotaService quotaService;

    @Override
    @Transactional
    public void processPayment(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Processing payment: id={}, orderId={}, eventType={}, status={}",
            payment.getId(), payment.getOrderId(), payment.getEventType(), payment.getStatus());
        
        String eventType = payment.getEventType();
        
        switch (eventType) {
            case "payment.captured", "order.paid" -> handlePaymentCaptured(payment);
            case "payment.authorized" -> handlePaymentAuthorized(payment);
            case "payment.failed" -> handlePaymentFailed(payment);
            case "refund.created" -> handleRefundCreated(payment);
            case "refund.processed" -> handleRefundProcessed(payment);
            case "payment.dispute.created" -> handleDisputeCreated(payment);
            default -> log.warn("[DefaultPaymentEventProcessor] Unhandled event type: {}", eventType);
        }
    }
    
    private void handlePaymentCaptured(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Payment captured for order: {}", payment.getOrderId());
        
        orderRepository.findByOrderId(payment.getOrderId()).ifPresent(order -> {
            if (order.getVerification() != PaymentVerification.SUCCESS) {
                orderRepository.updateVerificationByOrderId(payment.getOrderId(), PaymentVerification.SUCCESS);
                order.setVerification(PaymentVerification.SUCCESS);
            }

            if (order.getPurposeType() == PaymentPurpose.PLAN_UPGRADE
                    && order.getFulfillmentStatus() == FulfillmentStatus.UNFULFILLED) {
                log.info("[DefaultPaymentEventProcessor] Auto-fulfilling plan upgrade for order {} tenant {}",
                    payment.getOrderId(), order.getTenantId());
                AssignPlanRequest req = new AssignPlanRequest();
                req.setPlanId(order.getPurposeRefId());
                req.setOrderRef(order.getOrderRef());
                req.setResetUsage(true);
                quotaService.assignPlan(order.getTenantId(), req);
            }
        });
    }
    
    private void handlePaymentAuthorized(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Payment authorized for order: {}", payment.getOrderId());
    }
    
    private void handlePaymentFailed(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Payment failed for order: {}", payment.getOrderId());
        
        orderRepository.findByOrderId(payment.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.FAILED);
            order.setVerification(PaymentVerification.FAILED);
            orderRepository.save(order);
        });
    }
    
    private void handleRefundCreated(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Refund created for order: {}", payment.getOrderId());
    }
    
    private void handleRefundProcessed(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Refund processed for order: {}", payment.getOrderId());
        
        orderRepository.findByOrderId(payment.getOrderId()).ifPresent(order -> {
            orderRepository.updateFulfillmentStatusByOrderId(payment.getOrderId(), FulfillmentStatus.EXPIRED);

            if (order.getPurposeType() == PaymentPurpose.PLAN_UPGRADE
                    && order.getFulfillmentStatus() == FulfillmentStatus.FULFILLED) {
                if (order.getPreviousPlanId() != null) {
                    log.info("[DefaultPaymentEventProcessor] Reverting tenant {} to previous plan {} after refund on order {}",
                        order.getTenantId(), order.getPreviousPlanId(), payment.getOrderId());
                    AssignPlanRequest req = new AssignPlanRequest();
                    req.setPlanId(order.getPreviousPlanId());
                    req.setResetUsage(false);
                    quotaService.assignPlan(order.getTenantId(), req);
                } else {
                    log.warn("[DefaultPaymentEventProcessor] Refund on fulfilled plan upgrade order {} but no previousPlanId recorded. " +
                        "Tenant {} may need manual plan reversion.", payment.getOrderId(), order.getTenantId());
                }
            }
        });
    }

    private void handleDisputeCreated(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Dispute created for order: {}, marking for review",
            payment.getOrderId());
        orderRepository.findByOrderId(payment.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.DISPUTED);
            orderRepository.save(order);
        });
    }
}
