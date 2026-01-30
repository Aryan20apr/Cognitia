package com.intellidesk.cognitia.payments.models.dtos.razopayDtos;

import java.util.List;

import lombok.Data;

@Data
public class PaymentEntity {

    private String id;
    private String entity;
    private Integer amount;
    private String currency;
    private String status;
    private String order_id;
    private String invoice_id;
    private Boolean international;
    private String method;
    private Integer amount_refunded;
    private String refund_status;
    private Boolean captured;
    private String description;
    private String card_id;
    private String bank;
    private String wallet;
    private String vpa;
    private String email;
    private String contact;
    private List<Object> notes;
    private Integer fee;
    private Integer tax;
    private String error_code;
    private String error_description;
    private String error_source;
    private String error_step;
    private String error_reason;
    private AcquirerData acquirer_data;
    private Long created_at;
}
