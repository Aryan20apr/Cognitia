package com.intellidesk.cognitia.payments.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Configuration
public class PaymentConfiguration {

    @Value("${razorpay.api.key}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret}")
    private String razorpayApiSecret;
    
    @Bean
    public RazorpayClient getRazorpayClient() throws RazorpayException{

        return new RazorpayClient(razorpayApiKey,razorpayApiSecret);

    }
}
