package com.intellidesk.cognitia.payments.models.dtos.razopayDtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AcquirerData {
    private String bank_transaction_id;
}
