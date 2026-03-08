package com.intellidesk.cognitia.payments.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.payments.models.entities.Payment;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PaymentDeadLetterListener {

    @EventListener
    public void handle(PaymentDeadLetterEvent event) {
        Payment p = event.getPayment();
        log.error("[DEAD_LETTER] Payment {} for order {} exhausted all {} retries. " +
                "Event type: {}, Last error: {}",
            p.getId(), p.getOrderId(), p.getMaxAttempts(),
            p.getEventType(), p.getLastError());
    }
}
