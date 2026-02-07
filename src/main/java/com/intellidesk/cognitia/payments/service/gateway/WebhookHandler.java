package com.intellidesk.cognitia.payments.service.gateway;

import java.util.Map;

public interface WebhookHandler {
    
    /**
     * Verifies the webhook signature to ensure the request is from the payment gateway.
     * 
     * @param payload The raw payload string (must be raw for signature verification)
     * @param signature The signature header sent by the payment gateway
     * @return true if signature is valid, false otherwise
     */
    boolean verifyWebhookSignature(String payload, String signature);
    
    /**
     * Processes the payment webhook after signature verification.
     * 
     * @param payload The parsed webhook payload
     */
    void handlePaymentWebhook(Map<String, Object> payload, String eventId);
}
