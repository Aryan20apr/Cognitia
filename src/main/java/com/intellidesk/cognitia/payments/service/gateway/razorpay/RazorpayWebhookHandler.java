package com.intellidesk.cognitia.payments.service.gateway.razorpay;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.payments.events.PaymentReceivedEvent;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.Payload;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.PaymentEntity;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.RazorpayPayment;
import com.intellidesk.cognitia.payments.models.entities.Payment;
import com.intellidesk.cognitia.payments.models.enums.ProcessingStatus;
import com.intellidesk.cognitia.payments.repository.PaymentsRepository;
import com.intellidesk.cognitia.payments.service.gateway.WebhookHandler;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayWebhookHandler implements WebhookHandler {

    private final PaymentsRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            log.debug("[RazorpayWebhookHandler] Verifying webhook signature");
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                log.warn("[RazorpayWebhookHandler] Webhook signature verification failed");
            }
            return isValid;
        } catch (RazorpayException e) {
            log.error("[RazorpayWebhookHandler] Error verifying webhook signature: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void handlePaymentWebhook(Map<String, Object> payload, String eventId) {
        log.info("[RazorpayWebhookHandler] Processing webhook eventId: {}", eventId);
        
        
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(eventId);
        if (existing.isPresent()) {
            ProcessingStatus status = existing.get().getProcessingStatus();
            if (status == ProcessingStatus.COMPLETED || status == ProcessingStatus.PROCESSING) {
                log.info("[RazorpayWebhookHandler] Event {} already {}, skipping", eventId, status);
                return;
            }
            log.info("[RazorpayWebhookHandler] Event {} in status {}, will be retried", eventId, status);
            return;
        }
        
        
        RazorpayPayment razorpayPayment = objectMapper.convertValue(payload, RazorpayPayment.class);
        log.info("[RazorpayWebhookHandler] Parsed RazorpayPayment: {}", razorpayPayment.getEvent());

        Payload payloadDto = razorpayPayment.getPayload();
        PaymentEntity paymentEntity = payloadDto.getPayment().getEntity();

        // 3. Build Payment entity with PENDING status (not processed yet)
        Payment payment = Payment.builder()
            .paymentId(paymentEntity.getId())
            .orderId(paymentEntity.getOrder_id())
            .eventType(razorpayPayment.getEvent())
            .status(paymentEntity.getStatus())
            .processingStatus(ProcessingStatus.PROCESSING)  // Will be processed async
            .eventReceivedAt(OffsetDateTime.now())
            .amount(paymentEntity.getAmount() != null ? paymentEntity.getAmount().longValue() : 0L)
            .currency(paymentEntity.getCurrency())
            .amountRefunded(paymentEntity.getAmount_refunded() != null ? paymentEntity.getAmount_refunded().longValue() : 0L)
            .fee(paymentEntity.getFee() != null ? paymentEntity.getFee().longValue() : 0L)
            .tax(paymentEntity.getTax() != null ? paymentEntity.getTax().longValue() : 0L)
            .method(paymentEntity.getMethod())
            .captured(paymentEntity.getCaptured())
            .errorCode(paymentEntity.getError_code())
            .errorDescription(paymentEntity.getError_description())
            .idempotencyKey(eventId)  
            .rawPayload(payload)
            .attempts(0)
            .maxAttempts(5)
            .build();

        
        try {
            Payment savedPayment = paymentRepository.save(payment);
            log.info("[RazorpayWebhookHandler] Saved payment {} for event {}", savedPayment.getId(), eventId);
            
           
            eventPublisher.publishEvent(new PaymentReceivedEvent(this, savedPayment.getId(), eventId));
            log.info("[RazorpayWebhookHandler] Published PaymentReceivedEvent for payment {}", savedPayment.getId());
            
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation - another instance already inserted
            log.info("[RazorpayWebhookHandler] Concurrent insert for event {}, skipping", eventId);
        }
    }
    
}
