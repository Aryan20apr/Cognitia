package com.intellidesk.cognitia.payments.controller;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.payments.models.dtos.OrderCreationDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderDTO;
import com.intellidesk.cognitia.payments.service.gateway.PaymentGateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/order")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Order", description = "Payment order creation and management APIs")
public class OrderController {

    private final PaymentGateway paymentGateway;

    @Operation(
        summary = "Create Razorpay order",
        description = "Creates a new payment order in Razorpay and returns order details including orderId (Razorpay order ID) " +
                      "and orderRef (internal reference UUID). The orderRef should be stored client-side and used for payment verification."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Order created successfully",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Invalid order creation request"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createRazorpayOrder(@RequestBody OrderCreationDTO order) {
        
        OrderDTO newOrder = paymentGateway.createOrder(order);
        ApiResponse<OrderDTO> apiResponse = new ApiResponse<>("Order Created Successfully", true, newOrder);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getOrderStatus(@RequestParam String orderRef) {
        String status = paymentGateway.getOrderStatus(orderRef);
        JSONObject response = new JSONObject();
        response.put("status", status);
        return ResponseEntity.ok(new ApiResponse<>("Order Status", true, response));
    }
    
    
}
