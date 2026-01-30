package com.intellidesk.cognitia.payments.models.dtos;

import java.time.Instant;
import java.util.Map;

import com.intellidesk.cognitia.payments.models.enums.Gateway;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PaymentEvent {

    private Gateway gateway;
    private String eventType;
    private String paymentId;
    private String orderId;
    private String status;
    private Map<String,Object> rawPayload;
    private Instant timeStamp;

}
