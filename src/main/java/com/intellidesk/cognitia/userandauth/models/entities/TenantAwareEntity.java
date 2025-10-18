package com.intellidesk.cognitia.userandauth.models.entities;

import java.util.UUID;

import com.intellidesk.cognitia.userandauth.multiteancy.TenantEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
public abstract class TenantAwareEntity {
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
