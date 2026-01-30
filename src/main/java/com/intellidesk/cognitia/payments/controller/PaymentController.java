package com.intellidesk.cognitia.payments.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
    

    @PostMapping()
    public ResponseEntity<?> handlePayment(@RequestBody Map<String,Object> payload) {
        
       
        
        return null;
    }
    
}
