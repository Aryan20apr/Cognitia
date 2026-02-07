package com.intellidesk.cognitia.payments.models.dtos.razopayDtos;

import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RazorpayPayment {
    private String entity;
    private String account_id;
    private String event;
    private List<String> contains;
    private Payload payload;
    private Long created_at;
}
