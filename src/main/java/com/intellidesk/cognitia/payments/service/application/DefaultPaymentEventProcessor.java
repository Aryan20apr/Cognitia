package com.intellidesk.cognitia.payments.service.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.analytics.models.dto.AssignPlanRequest;
import com.intellidesk.cognitia.analytics.repository.PlanRepository;
import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.notification.EmailService;
import com.intellidesk.cognitia.payments.models.entities.Payment;
import com.intellidesk.cognitia.payments.models.entities.PaymentOrder;
import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;
import com.intellidesk.cognitia.payments.models.enums.OrderStatus;
import com.intellidesk.cognitia.payments.models.enums.PaymentPurpose;
import com.intellidesk.cognitia.payments.models.enums.PaymentVerification;
import com.intellidesk.cognitia.payments.repository.OrderRepository;
import com.intellidesk.cognitia.userandauth.repository.TenantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPaymentEventProcessor implements PaymentEventProcessor {

    private final OrderRepository orderRepository;
    private final QuotaService quotaService;
    private final EmailService emailService;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;

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

            sendPaymentSuccessEmail(order, payment);
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

            sendPaymentFailedEmail(order, payment);
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

            sendRefundEmail(order, payment);
        });
    }

    private void handleDisputeCreated(Payment payment) {
        log.info("[DefaultPaymentEventProcessor] Dispute created for order: {}, marking for review",
            payment.getOrderId());
        orderRepository.findByOrderId(payment.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.DISPUTED);
            orderRepository.save(order);

            sendDisputeAlertEmail(order, payment);
        });
    }

    private void sendPaymentSuccessEmail(PaymentOrder order, Payment payment) {
        try {
            String email = resolveTenantEmail(order.getTenantId());
            if (email == null) return;

            String planName = resolvePlanName(order.getPurposeRefId());
            String amount = formatAmount(payment.getAmount(), payment.getCurrency());

            emailService.sendHtml(email, "Payment successful — Cognitia", Constants.TEMPLATE_PAYMENT_SUCCESS,
                    Map.of(
                        "planName", planName,
                        "amount", amount,
                        "date", LocalDate.now().format(DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_DISPLAY)),
                        "orderId", order.getOrderRef()
                    ));
        } catch (Exception e) {
            log.error("Failed to send payment success email for order {}: {}", order.getOrderRef(), e.getMessage());
        }
    }

    private void sendPaymentFailedEmail(PaymentOrder order, Payment payment) {
        try {
            String email = resolveTenantEmail(order.getTenantId());
            if (email == null) return;

            String planName = resolvePlanName(order.getPurposeRefId());
            String amount = formatAmount(payment.getAmount(), payment.getCurrency());
            String reason = payment.getErrorDescription() != null ? payment.getErrorDescription() : "Payment could not be processed";

            emailService.sendHtml(email, "Payment failed — Cognitia", Constants.TEMPLATE_PAYMENT_FAILED,
                    Map.of(
                        "planName", planName,
                        "amount", amount,
                        "reason", reason
                    ));
        } catch (Exception e) {
            log.error("Failed to send payment failed email for order {}: {}", order.getOrderRef(), e.getMessage());
        }
    }

    private void sendRefundEmail(PaymentOrder order, Payment payment) {
        try {
            String email = resolveTenantEmail(order.getTenantId());
            if (email == null) return;

            Long refundedAmount = payment.getAmountRefunded() != null ? payment.getAmountRefunded() : payment.getAmount();
            emailService.sendHtml(email, "Refund processed — Cognitia", Constants.TEMPLATE_REFUND_PROCESSED,
                    Map.of(
                        "refundAmount", formatAmount(refundedAmount, payment.getCurrency()),
                        "originalAmount", formatAmount(payment.getAmount(), payment.getCurrency()),
                        "refundId", payment.getPaymentId(),
                        "date", LocalDate.now().format(DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_DISPLAY))
                    ));
        } catch (Exception e) {
            log.error("Failed to send refund email for order {}: {}", order.getOrderRef(), e.getMessage());
        }
    }

    private void sendDisputeAlertEmail(PaymentOrder order, Payment payment) {
        try {
            String email = resolveTenantEmail(order.getTenantId());
            if (email == null) return;

            emailService.sendSimple(email,
                    "[ALERT] Payment dispute created — Cognitia",
                    "A payment dispute has been created for order " + order.getOrderRef() +
                    " (Payment ID: " + payment.getPaymentId() + ", Amount: " +
                    formatAmount(payment.getAmount(), payment.getCurrency()) +
                    "). Please review immediately.");
        } catch (Exception e) {
            log.error("Failed to send dispute alert for order {}: {}", order.getOrderRef(), e.getMessage());
        }
    }

    private String resolveTenantEmail(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> t.getContactEmail())
                .orElse(null);
    }

    private String resolvePlanName(UUID planId) {
        if (planId == null) return Constants.FALLBACK_NA;
        return planRepository.findById(planId)
                .map(p -> p.getName())
                .orElse(Constants.FALLBACK_NA);
    }

    private String formatAmount(Long amountInPaise, String currency) {
        if (amountInPaise == null) return Constants.FALLBACK_NA;
        double amount = amountInPaise / 100.0;
        String symbol = Constants.CURRENCY_INR.equalsIgnoreCase(currency) ? Constants.CURRENCY_SYMBOL_INR : currency + " ";
        return symbol + String.format("%.2f", amount);
    }
}
