package com.intellidesk.cognitia.payments.service.application;

import com.intellidesk.cognitia.payments.models.dtos.PaymentEvent;

public interface PaymentEventProcessor {
    
    void process(PaymentEvent event);
}
