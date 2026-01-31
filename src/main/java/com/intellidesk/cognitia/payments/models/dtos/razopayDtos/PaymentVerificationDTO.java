package com.intellidesk.cognitia.payments.models.dtos.razopayDtos;

public record PaymentVerificationDTO(String paymentId, String orderRef, String signature) {

}
