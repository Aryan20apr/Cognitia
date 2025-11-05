package com.intellidesk.cognitia.analytics.models.dto;


import lombok.Data;
import java.util.UUID;

@Data
public class ChangePlanRequestDTO {
    private UUID newPlanId;
    private boolean immediate; // immediate change or at period end
    private boolean acceptProration; // if proration applies, tenant accepts charge
}
