package com.intellidesk.cognitia.payments.models.dtos;

import java.time.Instant;

import com.intellidesk.cognitia.payments.models.enums.Gateway;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PaymentEvent {

    private Gateway gateway;
    private String eventType;
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String gatewayStatus;
    private String rawPayload;
    private Instant occurredAt;

}
