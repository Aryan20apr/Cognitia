package com.intellidesk.cognitia.userandauth.multiteancy;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantEntityListener {
    
    @PrePersist
    @PreUpdate
    public void setTenant(TenantAwareEntity entity) {
        log.info("[TenantEntityListener] : [setTenant] : Setting tenantId for entity {}", entity.getClass().getSimpleName());
        entity.setTenantId(TenantContext.getTenantId());
    }
}
