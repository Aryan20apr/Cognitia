package com.intellidesk.cognitia.payments.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.payments.models.dtos.razopayDtos.PaymentVerificationDTO;
import com.intellidesk.cognitia.payments.service.gateway.PaymentGateway;
import com.intellidesk.cognitia.payments.service.gateway.WebhookHandler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment processing and verification APIs")
public class PaymentController {
    
    private final WebhookHandler webhookHandler;
    private final PaymentGateway paymentGateway;

    @Operation(
        summary = "Handle payment webhook",
        description = "Receives and processes payment webhook events from Razorpay"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Webhook processed successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Invalid webhook payload"
    )
    @PostMapping()
    public ResponseEntity<?> handlePayment(@RequestBody Map<String,Object> payload) {
        
       webhookHandler.handlePaymentWebhook(payload);
        
        return ResponseEntity.ok().build();
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
