package com.intellidesk.cognitia.payments.models.dtos;

import java.util.UUID;

import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;
import com.intellidesk.cognitia.payments.models.enums.OrderStatus;
import com.intellidesk.cognitia.payments.models.enums.PaymentVerification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderStatusDTO {
    private OrderStatus orderStatus;
    private PaymentVerification verification;
    private FulfillmentStatus fulfillmentStatus;
    private UUID planId;
}
