package com.intellidesk.cognitia.analytics.controllers;

import com.intellidesk.cognitia.analytics.models.dto.*;
import com.intellidesk.cognitia.analytics.service.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantQuotaController {

    private final QuotaService tenantQuotaService;

    @GetMapping("/{tenantId}/quota")
    public ResponseEntity<TenantQuotaDTO> getTenantQuota(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantQuotaService.getTenantQuota(tenantId));
    }

    @PostMapping("/{tenantId}/assign-plan")
    public ResponseEntity<TenantQuotaDTO> assignPlan(@PathVariable UUID tenantId,
                                                     @RequestBody AssignPlanRequest request) {
        return ResponseEntity.ok(tenantQuotaService.assignPlan(tenantId, request));
    }

    @PostMapping("/{tenantId}/quota/provision")
    public ResponseEntity<TenantQuotaDTO> provisionQuota(@PathVariable UUID tenantId,
                                                         @RequestBody QuotaProvisionRequest request) {
        return ResponseEntity.ok(tenantQuotaService.provisionQuota(tenantId, request));
    }
}