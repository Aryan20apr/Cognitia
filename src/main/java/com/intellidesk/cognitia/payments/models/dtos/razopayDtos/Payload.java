package com.intellidesk.cognitia.payments.models.dtos.razopayDtos;

public class Payload {
    private PaymentWrapper payment;

    public PaymentWrapper getPayment() {
        return payment;
    }

    public void setPayment(PaymentWrapper payment) {
        this.payment = payment;
    }
}
