package com.intellidesk.cognitia.payments.models.dtos;

import java.util.Map;
import java.util.UUID;

import com.intellidesk.cognitia.payments.models.enums.PaymentPurpose;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Schema(description = "Request DTO for creating a payment order")
public class OrderCreationDTO {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Schema(description = "Order amount in the smallest currency unit (e.g., paise for INR)", example = "29900")
    private Long amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-character ISO code")
    @Schema(description = "3-character ISO currency code", example = "INR")
    private String currency;

    @Schema(description = "Optional notes/metadata for the order")
    private Map<String, Object> notes;

    @Size(max = 100, message = "Receipt number must not exceed 100 characters")
    @Schema(description = "Optional receipt number for reference")
    private String receiptNo;

    @Schema(description = "Purpose of this payment (e.g., PLAN_UPGRADE, ADDON_PURCHASE). Optional for general payments.")
    private PaymentPurpose purposeType;

    @Schema(description = "Reference ID for the purpose (e.g., planId for PLAN_UPGRADE). Required when purposeType is set.")
    private UUID purposeRefId;
}