package com.intellidesk.cognitia.payments.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.PaymentVerificationDTO;
import com.intellidesk.cognitia.payments.service.gateway.PaymentGateway;
import com.intellidesk.cognitia.payments.service.gateway.WebhookHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment", description = "Payment processing and verification APIs")
public class PaymentController {
    
    private final WebhookHandler webhookHandler;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "Handle payment webhook",
        description = "Receives and processes payment webhook events from Razorpay. " +
                      "This endpoint is public but secured via webhook signature verification."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Webhook processed successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Invalid webhook signature"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Invalid webhook payload"
    )
    @PostMapping("/webhook")
    public ResponseEntity<?> handlePaymentWebhook(
            @RequestBody String rawPayload,
            @Parameter(description = "Razorpay webhook signature for verification")
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @Parameter(description = "Razorpay event ID")
            @RequestHeader(value = "x-razorpay-event-id", required = false) String eventId) {
        
        log.info("[PaymentController] Received webhook request");
        
        // Verify webhook signature
        if (signature == null || signature.isBlank()) {
            log.warn("[PaymentController] Missing X-Razorpay-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing signature header"));
        }
        
        if (!webhookHandler.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("[PaymentController] Webhook signature verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
        }
        
        log.info("[PaymentController] Webhook signature verified successfully");
        
        
        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<Map<String, Object>>() {});
            webhookHandler.handlePaymentWebhook(payload,eventId);
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            log.error("[PaymentController] Failed to parse webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid JSON payload"));
        }
    }

    @Operation(
        summary = "Verify payment signature",
        description = "Verifies the payment signature using Razorpay's signature verification algorithm. " +
                      "Retrieves the order_id from server using orderRef to prevent client-side tampering."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "202",
        description = "Payment verified successfully",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Payment verification failed - invalid signature or order not found",
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @PostMapping("/verify")
    public ResponseEntity<?>  verifyPayment(@RequestBody PaymentVerificationDTO payload) {
        
       Boolean b = paymentGateway.verifyPayment(payload); 
       ApiResponse<PaymentVerificationDTO> apiResponse = null;
       HttpStatus httpStatus = null;
       if(b){
        apiResponse = new ApiResponse<>("Payment Verfied", true, payload);
        httpStatus = HttpStatus.ACCEPTED;
       } else {
        apiResponse = new ApiResponse<>("Payment Verfication Failed", false, payload);
        httpStatus = HttpStatus.BAD_REQUEST;
       }

       return new ResponseEntity<>(apiResponse,httpStatus);
       
    }
    
    
}
