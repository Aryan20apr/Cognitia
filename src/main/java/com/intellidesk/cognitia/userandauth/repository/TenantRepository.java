package com.intellidesk.cognitia.userandauth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.userandauth.models.entities.Tenant;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
}
