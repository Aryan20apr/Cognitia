package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.userandauth.models.dtos.TenantDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Tenant;
import com.intellidesk.cognitia.userandauth.repository.TenantRepository;
import com.intellidesk.cognitia.userandauth.services.TenantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService{

    private final TenantRepository tenantRepository;

    public Boolean checkIfExists(String id){

       return tenantRepository.existsById(UUID.fromString(id));
    }
    
    public Tenant createTenant(TenantDTO tenantDTO){

    log.info("[TenantServiceImpl] [createTenant] creating tenant with info: {}", tenantDTO);

        Tenant tenant = Tenant.builder()
                        .name(tenantDTO.name())
                        .about(tenantDTO.about())
                        .contactEmail(tenantDTO.contactEmail())
                        .users(new HashSet<>())
                        .domain(tenantDTO.domain()).build();
        
        return tenantRepository.save(tenant);
    }

    public Tenant getTenant(String tenantId){

        Optional<Tenant> optionalTenant =  tenantRepository.findById(UUID.fromString(tenantId));

        if(optionalTenant.isEmpty()){
            throw new RuntimeException("Company not found with company id:"+tenantId);
        }
        return optionalTenant.get();
    }

    @Override
    public List<Tenant> getAllCompanies() {
        
        return tenantRepository.findAll();
    }
}
