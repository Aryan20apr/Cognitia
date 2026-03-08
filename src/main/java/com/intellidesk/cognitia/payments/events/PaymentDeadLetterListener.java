package com.intellidesk.cognitia.payments.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.notification.EmailService;
import com.intellidesk.cognitia.payments.models.entities.Payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentDeadLetterListener {

    private final EmailService emailService;

    @Value("${spring.mail.username:}")
    private String adminEmail;

    @EventListener
    public void handle(PaymentDeadLetterEvent event) {
        Payment p = event.getPayment();
        log.error("[DEAD_LETTER] Payment {} for order {} exhausted all {} retries. " +
                "Event type: {}, Last error: {}",
            p.getId(), p.getOrderId(), p.getMaxAttempts(),
            p.getEventType(), p.getLastError());

        if (adminEmail != null && !adminEmail.isBlank()) {
            emailService.sendSimple(adminEmail,
                "[CRITICAL] Payment dead letter — Cognitia",
                "Payment " + p.getId() + " for order " + p.getOrderId() +
                " exhausted all " + p.getMaxAttempts() + " retries.\n" +
                "Event type: " + p.getEventType() + "\n" +
                "Last error: " + p.getLastError() + "\n\n" +
                "Please investigate immediately.");
        }
    }
}
