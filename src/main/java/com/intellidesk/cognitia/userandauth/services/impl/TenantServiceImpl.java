package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.AssignPlanRequest;
import com.intellidesk.cognitia.analytics.models.dto.PlanDTO;
import com.intellidesk.cognitia.analytics.service.PlanCatalogService;
import com.intellidesk.cognitia.analytics.service.QuotaService;
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
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserService userService;
    private final QuotaService quotaService;
    private final PlanCatalogService planCatalogService;

    @Override
    public Boolean checkIfExists(String id) {
        return tenantRepository.existsById(UUID.fromString(id));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
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


        assignDefaultPlan(newTenant.getId());
        
        return mapToDTO(newTenant);
    }

    private void assignDefaultPlan(UUID tenantId) {
        // Find default plan via service (try free -> starter -> TRIAL001)
        PlanDTO defaultPlan = findDefaultPlan();
        
        AssignPlanRequest request = new AssignPlanRequest();
        request.setPlanId(defaultPlan.getId());
        request.setResetUsage(true);
        
        quotaService.assignPlan(tenantId, request);
        log.info("[TenantServiceImpl] Assigned default plan {} to tenant {}", defaultPlan.getCode(), tenantId);
    }

    private PlanDTO findDefaultPlan() {
        
            try {
                PlanDTO plan = planCatalogService.findByCode("TRIAL001");
                if (plan != null) {
                    return plan;
                }
            } catch (Exception error) {
                throw new ApiException("Plan not found");
            }
        
        throw new IllegalStateException("No default plan configured. Please create a plan with code 'free', 'starter', or 'TRIAL001'");
    }

    @Override
    public TenantDTO getTenant(String tenantId) {

        Optional<Tenant> optionalTenant = tenantRepository.findById(UUID.fromString(tenantId));

        if (optionalTenant.isEmpty()) {
            throw new RuntimeException("Company not found with company id:" + tenantId);
        }

        Tenant tenant = optionalTenant.get();
        return mapToDTO(tenant);
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
