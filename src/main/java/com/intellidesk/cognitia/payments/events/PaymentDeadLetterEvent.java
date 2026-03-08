package com.intellidesk.cognitia.payments.events;

import org.springframework.context.ApplicationEvent;

import com.intellidesk.cognitia.payments.models.entities.Payment;

import lombok.Getter;

@Getter
public class PaymentDeadLetterEvent extends ApplicationEvent {

    private final Payment payment;

    public PaymentDeadLetterEvent(Object source, Payment payment) {
        super(source);
        this.payment = payment;
    }
}
