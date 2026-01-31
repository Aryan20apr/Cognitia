package com.intellidesk.cognitia.payments.service.application;

import com.intellidesk.cognitia.payments.models.dtos.PaymentEvent;
import org.springframework.stereotype.Service;
@Service
public class DefaultPaymentEventProcessor implements PaymentEventProcessor {

    @Override
    public void process(PaymentEvent event) {
        
    }
    
}
