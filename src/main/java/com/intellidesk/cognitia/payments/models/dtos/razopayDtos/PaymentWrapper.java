package com.intellidesk.cognitia.payments.models.dtos.razopayDtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentWrapper {
    private PaymentEntity entity;

    public PaymentEntity getEntity() {
        return entity;
    }

    public void setEntity(PaymentEntity entity) {
        this.entity = entity;
    }
}
