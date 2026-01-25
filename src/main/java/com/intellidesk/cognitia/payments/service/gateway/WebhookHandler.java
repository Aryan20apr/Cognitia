package com.intellidesk.cognitia.payments.service.gateway;

import java.util.Map;

public interface WebhookHandler {
    
    public void handlePaymentWebhook(Map<String,Object> paymentEvent);
}
