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

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    
    private final WebhookHandler webhookHandler;
    private final PaymentGateway paymentGateway;

    @PostMapping()
    public ResponseEntity<?> handlePayment(@RequestBody Map<String,Object> payload) {
        
       webhookHandler.handlePaymentWebhook(payload);
        
        return ResponseEntity.ok().build();
    }

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
