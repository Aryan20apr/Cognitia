package com.intellidesk.cognitia.payments.models.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Builder
public class OrderDTO {
    
    private String orderId;

    private String orderRef;

    private Long amount;

    private Long amountPaid;

    private Long amountDue;

    private String currency;

    private String status;

    private String createdAt;

    private String updatedAt;

}
