package com.intellidesk.cognitia.payments.service.gateway.razorpay;

import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.payments.models.dtos.OrderCreationDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderDTO;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.PaymentVerificationDTO;
import com.intellidesk.cognitia.payments.models.entities.PaymentOrder;
import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;
import com.intellidesk.cognitia.payments.models.enums.OrderStatus;
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
            .status(order.get("status"))
            .rawOrder(order.toJson().toMap())
            // Purpose tracking fields
            .purposeType(orderCreationDTO.getPurposeType())
            .purposeRefId(orderCreationDTO.getPurposeRefId())
            .fulfillmentStatus(FulfillmentStatus.UNFULFILLED)
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
    public Boolean verifyPayment(PaymentVerificationDTO paymentVerificationDTO) {
      log.info("[RazorpayGateway] [verifyPayment] Received PaymentVerificationDTO: {}", paymentVerificationDTO);
      Optional<String> optionalOrder =  orderRepository.findOrderIdByOrderRef(paymentVerificationDTO.orderRef());

      if(!optionalOrder.isPresent()){
        throw new ApiException("Order not found");
      }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", optionalOrder.get());
        options.put("razorpay_payment_id", paymentVerificationDTO.paymentId());
        options.put("razorpay_signature", paymentVerificationDTO.signature());

        try {
            Boolean res = Utils.verifyPaymentSignature(options, razorpayApiSecret);
            if(res){
                orderRepository.updateVerificationByOrderRef(paymentVerificationDTO.orderRef(), PaymentVerification.SUCCESS);
            } else {
                orderRepository.updateVerificationByOrderRef(paymentVerificationDTO.orderRef(), PaymentVerification.FAILED);
            }
            return res;
        } catch (RazorpayException ex) {
            log.error("Exception occured during payment verification: {}",ex.getMessage());
            orderRepository.updateVerificationByOrderRef(paymentVerificationDTO.orderRef(), PaymentVerification.FAILED);
            return false;
        }
    }


    @Override
    public String getOrderStatus(String orderRef) {
        Optional<PaymentOrder> order = orderRepository.findByOrderRef(orderRef);
        if(!order.isPresent()){
         log.error("[RazorpayGateway] [getPaymentStatus] Order not found for orderRef: {}", orderRef);
           return "FAILED";
        }
        PaymentOrder paymentOrder = order.get();
        return paymentOrder.getStatus().name();
    }

}
