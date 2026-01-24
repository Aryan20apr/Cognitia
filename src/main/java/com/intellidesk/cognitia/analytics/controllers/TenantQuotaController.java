package com.intellidesk.cognitia.analytics.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.analytics.models.dto.AssignPlanRequest;
import com.intellidesk.cognitia.analytics.models.dto.QuotaProvisionRequest;
import com.intellidesk.cognitia.analytics.models.dto.TenantQuotaDTO;
import com.intellidesk.cognitia.analytics.service.QuotaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Quotas", description = "Manage tenant quotas and plans")
public class TenantQuotaController {

    private final QuotaService tenantQuotaService;

    @Operation(summary = "Get tenant quota")
    @GetMapping("/{tenantId}/quota")
    public ResponseEntity<TenantQuotaDTO> getTenantQuota(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantQuotaService.getTenantQuota(tenantId));
    }

    @Operation(summary = "Assign a plan to tenant")
    @PostMapping("/{tenantId}/assign-plan")
    public ResponseEntity<TenantQuotaDTO> assignPlan(@PathVariable UUID tenantId,
                                                     @RequestBody AssignPlanRequest request) {
        return ResponseEntity.ok(tenantQuotaService.assignPlan(tenantId, request));
    }

    @Operation(summary = "Provision custom quota")
    @PostMapping("/{tenantId}/quota/provision")
    public ResponseEntity<TenantQuotaDTO> provisionQuota(@PathVariable UUID tenantId,
                                                         @RequestBody QuotaProvisionRequest request) {
        return ResponseEntity.ok(tenantQuotaService.provisionQuota(tenantId, request));
    }
}