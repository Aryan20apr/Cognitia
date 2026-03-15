package com.intellidesk.cognitia.payments.service.gateway.razorpay;

import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.AssignPlanRequest;
import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.payments.models.dtos.OrderCreationDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderStatusDTO;
import com.intellidesk.cognitia.payments.models.dtos.VerificationResultDTO;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.PaymentVerificationDTO;
import com.intellidesk.cognitia.payments.models.entities.PaymentOrder;
import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;
import com.intellidesk.cognitia.payments.models.enums.OrderStatus;
import com.intellidesk.cognitia.payments.models.enums.PaymentPurpose;
import com.intellidesk.cognitia.payments.models.enums.PaymentStatus;
import com.intellidesk.cognitia.payments.models.enums.PaymentVerification;
import com.intellidesk.cognitia.payments.repository.OrderRepository;
import com.intellidesk.cognitia.payments.service.gateway.PaymentGateway;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class RazorpayGateway implements PaymentGateway {

    private final RazorpayClient razorpayClient;
    private final OrderRepository orderRepository;
    private final QuotaService quotaService;
    
    @Value("${razorpay.api.secret}")
    private String razorpayApiSecret;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderCreationDTO orderCreationDTO) {

        // TODO: Decouple the order persistance from the gateway, instead return complete gateway agnostic orderdto to a parent service layer`

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", orderCreationDTO.getAmount());
        orderRequest.put("currency", orderCreationDTO.getCurrency());
        try {
            UUID tenantId = TenantContext.getTenantId();
            Order order = razorpayClient.orders.create(orderRequest);
            log.info("[RazorpayGateway] [createOrder] Razorpay order created: {}", order.toJson().toString());
            PaymentOrder paymentOrder = mapToPaymentOrder(order, tenantId, orderCreationDTO);

            PaymentOrder newPaymentOrder = orderRepository.save(paymentOrder);
            return mapToOrderDTO(newPaymentOrder);
        } catch (RazorpayException ex) {
            log.error("[RazorpayGateway] [createOrder] Error while creating razorpay order: {}",ex.getMessage());
            throw new ApiException("Failed to create order", ex, null);
        }
      
    }

    private OrderDTO mapToOrderDTO(PaymentOrder paymentOrder) {
        return OrderDTO.builder()
            .orderId(paymentOrder.getOrderId())
            .orderRef(paymentOrder.getOrderRef())
            .amount(paymentOrder.getAmount())
            .amountPaid(paymentOrder.getAmountPaid())
            .amountDue(paymentOrder.getAmountDue())
            .currency(paymentOrder.getCurrency())
            .status(paymentOrder.getStatus().name())
            .createdAt(paymentOrder.getCreatedAt() != null ? paymentOrder.getCreatedAt().toString() : null)
            .updatedAt(paymentOrder.getUpdatedAt() != null ? paymentOrder.getUpdatedAt().toString() : null)
            .build();
    }
    private PaymentOrder mapToPaymentOrder(Order order, UUID tenantId, OrderCreationDTO orderCreationDTO) {

        if(order.get("error") != null){
            log.error("[RazorpayGateway] [mapToPaymentOrder] Error from Razorpay order response: {}", order.toJson().toString());
            throw new ApiException("Failed to create payment order !", order.toJson().toMap());
        }


        PaymentOrder paymentOrder = PaymentOrder.builder()
            .orderId(order.get("id"))
            .orderRef(UUID.randomUUID().toString())
            .amount(((Number) order.get("amount")).longValue())
            .amountDue(((Number)order.get("amount_due")).longValue())
            .amountPaid(((Number)order.get("amount_paid")).longValue())
            .attempts(order.get("attempts"))
            .createdAt(((Date)(order.get("created_at")))
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toOffsetDateTime())
            .currency(order.get("currency"))
            .notes(convertNotesToString(order.get("notes")))
            .status(OrderStatus.valueOf(((String)order.get("status")).toUpperCase()))
            .rawOrder(order.toJson().toMap())
            // Purpose tracking fields
            .purposeType(orderCreationDTO.getPurposeType())
            .purposeRefId(orderCreationDTO.getPurposeRefId())
            .fulfillmentStatus(FulfillmentStatus.UNFULFILLED)
            .paymentStatus(PaymentStatus.CREATED)
            .verification(PaymentVerification.PENDING)
            .build();

            paymentOrder.setTenantId(tenantId);
            return paymentOrder;
    }

    private String convertNotesToString(Object notes) {
        if (notes == null) {
            return null;
        }
        if (notes instanceof JSONObject jsonObject) {
            return jsonObject.toString();
        }
        if (notes instanceof JSONArray jsonArray) {
            return jsonArray.toString();
        }
        if (notes instanceof String string) {
            return string;
        }
        // For any other type, convert to string
        return notes.toString();
    }

    @Override
    @Transactional
    public VerificationResultDTO verifyPayment(PaymentVerificationDTO paymentVerificationDTO) {
      log.info("[RazorpayGateway] [verifyPayment] Received PaymentVerificationDTO: {}", paymentVerificationDTO);
      PaymentOrder order = orderRepository.findByOrderRef(paymentVerificationDTO.orderRef())
              .orElseThrow(() -> new ApiException("Order not found"));

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", order.getOrderId());
        options.put("razorpay_payment_id", paymentVerificationDTO.paymentId());
        options.put("razorpay_signature", paymentVerificationDTO.signature());

        boolean verified;
        try {
            verified = Utils.verifyPaymentSignature(options, razorpayApiSecret);
        } catch (RazorpayException ex) {
            log.error("Exception occured during payment verification: {}", ex.getMessage());
            verified = false;
        }

        if (!verified) {
            orderRepository.updateVerificationByOrderRef(paymentVerificationDTO.orderRef(), PaymentVerification.FAILED);
            return VerificationResultDTO.builder()
                    .verified(false)
                    .fulfillmentStatus(order.getFulfillmentStatus())
                    .build();
        }

        orderRepository.updateVerificationByOrderRef(paymentVerificationDTO.orderRef(), PaymentVerification.SUCCESS);
        order.setVerification(PaymentVerification.SUCCESS);

        return attemptFulfillment(order);
    }

    private VerificationResultDTO attemptFulfillment(PaymentOrder order) {
        if (order.getPurposeType() != PaymentPurpose.PLAN_UPGRADE
                || order.getFulfillmentStatus() != FulfillmentStatus.UNFULFILLED) {
            return VerificationResultDTO.builder()
                    .verified(true)
                    .fulfillmentStatus(order.getFulfillmentStatus())
                    .planId(order.getPurposeRefId())
                    .build();
        }

        try {
            log.info("[RazorpayGateway] Attempting synchronous plan fulfillment for order {} tenant {}",
                    order.getOrderRef(), order.getTenantId());
            AssignPlanRequest req = new AssignPlanRequest();
            req.setPlanId(order.getPurposeRefId());
            req.setOrderRef(order.getOrderRef());
            req.setResetUsage(true);
            if (true) throw new RuntimeException("Simulated: quota service unavailable");
            quotaService.assignPlan(order.getTenantId(), req);

            return VerificationResultDTO.builder()
                    .verified(true)
                    .fulfillmentStatus(FulfillmentStatus.FULFILLED)
                    .planId(order.getPurposeRefId())
                    .build();
        } catch (Exception ex) {
            log.error("[RazorpayGateway] Synchronous fulfillment failed for order {}. " +
                    "Webhook will retry. Error: {}", order.getOrderRef(), ex.getMessage());
            return VerificationResultDTO.builder()
                    .verified(true)
                    .fulfillmentStatus(FulfillmentStatus.UNFULFILLED)
                    .planId(order.getPurposeRefId())
                    .fulfillmentError("Plan activation is being processed. It will be ready shortly.")
                    .build();
        }
    }


    @Override
    public OrderStatusDTO getOrderStatus(String orderRef) {
        PaymentOrder paymentOrder = orderRepository.findByOrderRef(orderRef)
                .orElseThrow(() -> new ApiException("Order not found for orderRef: " + orderRef));

        return OrderStatusDTO.builder()
                .orderStatus(paymentOrder.getStatus())
                .verification(paymentOrder.getVerification())
                .fulfillmentStatus(paymentOrder.getFulfillmentStatus())
                .planId(paymentOrder.getPurposeRefId())
                .build();
    }

}
