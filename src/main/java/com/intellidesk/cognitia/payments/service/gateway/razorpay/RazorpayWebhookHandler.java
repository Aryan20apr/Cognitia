package com.intellidesk.cognitia.payments.service.gateway.razorpay;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.payments.models.dtos.PaymentEvent;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.Payload;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.PaymentEntity;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.RazorpayPayment;
import com.intellidesk.cognitia.payments.models.enums.Gateway;
import com.intellidesk.cognitia.payments.service.application.PaymentEventProcessor;
import com.intellidesk.cognitia.payments.service.gateway.WebhookHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayWebhookHandler implements WebhookHandler{

    private final PaymentEventProcessor paymentEventProcessor;
    private final ObjectMapper objectMapper;
    @Override
    public void handlePaymentWebhook(Map<String, Object> payload) {
        RazorpayPayment razorpayPayment =
            objectMapper.convertValue(payload, RazorpayPayment.class);

        Payload payloadDto = razorpayPayment.getPayload();
        PaymentEntity paymentEntity = payloadDto.getPayment().getEntity();

        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setGateway(Gateway.RAZORPAY);
        paymentEvent.setEventType(razorpayPayment.getEvent());
        paymentEvent.setOrderId(paymentEntity.getOrder_id());
        paymentEvent.setPaymentId(paymentEntity.getId());
        paymentEvent.setStatus(paymentEntity.getStatus());
        paymentEvent.setRawPayload(payload);
        paymentEvent.setTimeStamp(Instant.ofEpochSecond(razorpayPayment.getCreated_at()));

    }
    
}
