package com.intellidesk.cognitia.userandauth.services;

import java.util.List;

import java.util.UUID;

import com.intellidesk.cognitia.userandauth.models.dtos.TenantDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.TenantUpdateDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Tenant;

public interface TenantService {

    public Boolean checkIfExists(String tenantId);
    
    public TenantDTO createTenant(TenantDTO tenantDTO);

    public TenantDTO getTenant(String companyId);

    public List<Tenant> getAllCompanies();

    public TenantDTO updateTenant(UUID tenantId, TenantUpdateDTO tenantUpdateDTO);
}
