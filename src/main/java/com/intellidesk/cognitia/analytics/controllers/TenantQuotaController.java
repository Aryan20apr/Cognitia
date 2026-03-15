package com.intellidesk.cognitia.analytics.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import com.intellidesk.cognitia.analytics.models.dto.AssignPlanRequest;
import com.intellidesk.cognitia.analytics.models.dto.DowngradePlanRequest;
import com.intellidesk.cognitia.analytics.models.dto.QuotaProvisionRequest;
import com.intellidesk.cognitia.analytics.models.dto.TenantQuotaDTO;
import com.intellidesk.cognitia.analytics.service.QuotaService;
import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Quotas", description = "Manage tenant quotas and plans")
public class TenantQuotaController {

    private final QuotaService tenantQuotaService;

    @Operation(summary = "Get tenant quota")
    @GetMapping("/{tenantId}/quota")
    @PreAuthorize("hasAuthority('PERM_QUOTA_READ')")
    public ResponseEntity<TenantQuotaDTO> getTenantQuota(@PathVariable UUID tenantId) {
        validateTenantAccess(tenantId);
        return ResponseEntity.ok(tenantQuotaService.getTenantQuota(tenantId));
    }

    @Operation(summary = "Assign a plan to tenant (platform admin only)")
    @PostMapping("/{tenantId}/assign-plan")
    @PreAuthorize("hasAuthority('PERM_QUOTA_ADMIN')")
    public ResponseEntity<TenantQuotaDTO> assignPlan(@PathVariable UUID tenantId,
                                                     @RequestBody @Valid AssignPlanRequest request) {
        validateTenantAccess(tenantId);
        return ResponseEntity.ok(tenantQuotaService.assignPlan(tenantId, request));
    }

    @Operation(summary = "Provision custom quota (platform admin only)")
    @PostMapping("/{tenantId}/quota/provision")
    @PreAuthorize("hasAuthority('PERM_QUOTA_ADMIN')")
    public ResponseEntity<TenantQuotaDTO> provisionQuota(@PathVariable UUID tenantId,
                                                         @RequestBody QuotaProvisionRequest request) {
        validateTenantAccess(tenantId);
        return ResponseEntity.ok(tenantQuotaService.provisionQuota(tenantId, request));
    }

    @Operation(summary = "Downgrade to a lower plan")
    @PostMapping("/{tenantId}/downgrade")
    @PreAuthorize("hasAuthority('PERM_QUOTA_READ')")
    public ResponseEntity<TenantQuotaDTO> downgradePlan(@PathVariable UUID tenantId,
                                                        @RequestBody @Valid DowngradePlanRequest request) {
        validateTenantAccess(tenantId);
        return ResponseEntity.ok(tenantQuotaService.downgradePlan(tenantId, request.getPlanId()));
    }

    private void validateTenantAccess(UUID tenantId) {
        UUID currentTenant = TenantContext.getTenantId();
        if (Constants.PLATFORM_TENANT_ID.equals(currentTenant)) {
            return;
        }
        if (!tenantId.equals(currentTenant)) {
            throw new AccessDeniedException("Cannot access resources for another tenant");
        }
    }
}