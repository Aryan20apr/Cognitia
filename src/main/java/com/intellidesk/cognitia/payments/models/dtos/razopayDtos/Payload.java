package com.intellidesk.cognitia.payments.models.dtos.razopayDtos;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payload {
   
    private PaymentWrapper payment;

    public PaymentWrapper getPayment() {
        return payment;
    }

    public void setPayment(PaymentWrapper payment) {
        this.payment = payment;
    }
}
