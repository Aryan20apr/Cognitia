package com.intellidesk.cognitia.payments.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.payments.models.dtos.OrderCreationDTO;
import com.intellidesk.cognitia.payments.models.dtos.OrderDTO;
import com.intellidesk.cognitia.payments.service.gateway.PaymentGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/v1/order")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final PaymentGateway paymentGateway;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createRazorpayOrder(@RequestBody OrderCreationDTO order) {
        
        OrderDTO newOrder = paymentGateway.createOrder(order);
        ApiResponse<OrderDTO> apiResponse = new ApiResponse<>("Order Created Successfully", true, newOrder);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
    
    
}
