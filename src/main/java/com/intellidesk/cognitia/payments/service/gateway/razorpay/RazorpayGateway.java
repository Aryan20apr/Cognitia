package com.intellidesk.cognitia.payments.service.gateway.razorpay;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.payments.models.dtos.OrderCreationDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderDTO;
import com.intellidesk.cognitia.payments.models.entities.PaymentOrder;
import com.intellidesk.cognitia.payments.repository.OrderRepository;
import com.intellidesk.cognitia.payments.service.gateway.PaymentGateway;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class RazorpayGateway implements PaymentGateway {

    private final RazorpayClient razorpayClient;
    OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderCreationDTO orderCreationDTO) {

        // TODO: Decouple the order persistance from the gateway, instead return complete gateway agnostic orderdto to a parent service layer`
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", orderCreationDTO.getAmount() * 100);
        orderRequest.put("currency", orderCreationDTO.getCurrency());
        try {
            UUID tenantId = TenantContext.getTenantId();
            Order order = razorpayClient.orders.create(orderRequest);

            PaymentOrder paymentOrder = mapToPaymentOrder(order, tenantId);

            PaymentOrder newPaymentOrder = orderRepository.save(paymentOrder);
            return mapToOrderDTO(newPaymentOrder);
        } catch (RazorpayException ex) {
            log.error("[RazorpayGateway] [createOrder] Error while creating razorpay order: {}",ex.getMessage());
        }
        return null;
    }

    private OrderDTO mapToOrderDTO(PaymentOrder paymentOrder) {
        return OrderDTO.builder()
            .orderId(paymentOrder.getOrderId())
            .orderRef(paymentOrder.getOrderRef())
            .amount(paymentOrder.getAmount())
            .amountPaid(paymentOrder.getAmountPaid())
            .amountDue(paymentOrder.getAmountDue())
            .currency(paymentOrder.getCurrency())
            .status(paymentOrder.getStatus())
            .createdAt(paymentOrder.getCreatedAt() != null ? paymentOrder.getCreatedAt().toString() : null)
            .updatedAt(paymentOrder.getUpdatedAt() != null ? paymentOrder.getUpdatedAt().toString() : null)
            .build();
    }
    private PaymentOrder mapToPaymentOrder(Order order, UUID tenantId) {

        if(order.get("error") != null){
            log.error("[RazorpayGateway] [mapToPaymentOrder] Error from Razorpay order response: {}", order.toJson().toString());
            throw new ApiException("Failed to create payment order !", order.toJson().toMap());
        }


        PaymentOrder paymentOrder = PaymentOrder.builder()
            .orderId(order.get("id"))
            .amount(order.get("amount"))
            .amountDue(order.get("amount_due"))
            .amountPaid(order.get("amount_paid"))
            .attempts(order.get("attempts"))
            .createdAt(OffsetDateTime.ofInstant(Instant.ofEpochSecond(order.get("created_at")), ZoneId.systemDefault()))
            .currency(order.get("currency"))
            .notes(order.get("notes"))
            .status(order.get("status"))
            .rawOrder(order.toJson().toMap())
            .build();

            paymentOrder.setTenantId(tenantId);
            return paymentOrder;
    }

}
