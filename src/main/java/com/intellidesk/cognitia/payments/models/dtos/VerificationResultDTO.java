package com.intellidesk.cognitia.payments.models.dtos;

import java.util.UUID;

import com.intellidesk.cognitia.payments.models.enums.FulfillmentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerificationResultDTO {
    private boolean verified;
    private FulfillmentStatus fulfillmentStatus;
    private UUID planId;
    private String fulfillmentError;
}
