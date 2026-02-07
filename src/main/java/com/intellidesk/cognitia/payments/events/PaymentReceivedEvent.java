package com.intellidesk.cognitia.payments.events;

import java.util.UUID;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class PaymentReceivedEvent extends ApplicationEvent {
    
    private final UUID paymentId;
    private final String eventId;  // Razorpay event ID for logging
    
    public PaymentReceivedEvent(Object source, UUID paymentId, String eventId) {
        super(source);
        this.paymentId = paymentId;
        this.eventId = eventId;
    }
}
