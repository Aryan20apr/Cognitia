package com.intellidesk.cognitia.userandauth.multiteancy;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void setTenantId(UUID tenantId) {
        log.info("[TenantContext] : [setTenantId] : Setting tenantId to {}", tenantId);
        currentTenant.set(tenantId);
    }

    public static UUID getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        log.info("[TenantContext] : [clear] : Clearing tenantId (was {})", currentTenant.get());
        currentTenant.remove();
    }
}
