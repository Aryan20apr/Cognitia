package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.userandauth.models.dtos.RoleCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.TenantDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Tenant;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.models.entities.enums.RoleEnum;
import com.intellidesk.cognitia.userandauth.repository.TenantRepository;
import com.intellidesk.cognitia.userandauth.services.TenantService;
import com.intellidesk.cognitia.userandauth.services.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserService userService;

    public Boolean checkIfExists(String id) {

        return tenantRepository.existsById(UUID.fromString(id));
    }

    @Transactional
    public TenantDTO createTenant(TenantDTO tenantDTO){

    log.info("[TenantServiceImpl] [createTenant] creating tenant with info: {}", tenantDTO);

        Tenant tenant = Tenant.builder()
                        .name(tenantDTO.getName())
                        .about(tenantDTO.getAbout())
                        .contactEmail(tenantDTO.getContactEmail())
                        .users(new HashSet<>())
                        .domain(tenantDTO.getDomain()).build();
        Tenant newTenant = tenantRepository.save(tenant);
        RoleCreationDTO roleCreationDTO = new RoleCreationDTO();
        roleCreationDTO.setName(RoleEnum.SUPER_ADMIN.toString());
        UserCreationDTO userCreationDTO = new UserCreationDTO(
            tenantDTO.getAdminName(),
            tenantDTO.getAdminPassword(),
            tenantDTO.getAdminEmail(),
            newTenant.getId().toString(),
            tenantDTO.getPhoneNumber(),
            roleCreationDTO);
        
        UserDetailsDTO user = userService.createUser(userCreationDTO);
        User tempUser = new User();
        tempUser.setId(UUID.fromString(user.getUserId()));
        tempUser.setEmail(user.getEmail());
        tempUser.setName(user.getName());
        tempUser.setPhoneNumber(user.getPhoneNumber());
        newTenant.setRootUser(tempUser);
        
        return mapToDTO(newTenant);
    }

    public Tenant getTenant(String tenantId) {

        Optional<Tenant> optionalTenant = tenantRepository.findById(UUID.fromString(tenantId));

        if (optionalTenant.isEmpty()) {
            throw new RuntimeException("Company not found with company id:" + tenantId);
        }
        return optionalTenant.get();
    }

    @Override
    public List<Tenant> getAllCompanies() {

        return tenantRepository.findAll();
    }

    private TenantDTO mapToDTO(Tenant tenant){
        return new TenantDTO(
            tenant.getId().toString(),
            tenant.getName(),
            tenant.getAbout(),
            tenant.getDomain(),
            tenant.getContactEmail(),
            tenant.getRootUser().getEmail(),
            "-",
            tenant.getRootUser().getName(),
            tenant.getRootUser().getPhoneNumber()
        );
    }
}
