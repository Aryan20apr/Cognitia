package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.notification.EmailService;
import com.intellidesk.cognitia.notification.OtpService;
import com.intellidesk.cognitia.userandauth.models.dtos.UserCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Permission;
import com.intellidesk.cognitia.userandauth.models.entities.Role;
import com.intellidesk.cognitia.userandauth.models.entities.Tenant;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.models.entities.enums.RoleEnum;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.repository.PermissionsRepository;
import com.intellidesk.cognitia.userandauth.repository.RoleRepository;
import com.intellidesk.cognitia.userandauth.repository.TenantRepository;
import com.intellidesk.cognitia.userandauth.repository.UserRepository;
import com.intellidesk.cognitia.userandauth.services.UserService;
import com.intellidesk.cognitia.utils.Utils;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PermissionsRepository permissionsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5137}")
    private String frontendUrl;


    public List<User> getAll() {
        return userRepository.findAll(); // tenant filter automatically applied
    }

    @Override
    @Transactional
    public UserDetailsDTO createUser(UserCreationDTO userCreationDTO) {
        
       
        log.info("[UserServiceImpl] [createUser] UserCreationDTO received: {}", userCreationDTO);
        User user = new User();

        String email = userCreationDTO.email();
        String phone = userCreationDTO.phoneNumber();

        if(userRepository.existsGloballyByEmail(email)){
            throw new ApiException("A user with this email already exists");
        }
        if(userRepository.existsGloballyByPhoneNumber(phone)){
            throw new ApiException("A user with this phone number already exists");
        }

        UUID companyId = TenantContext.getTenantId();
        if (companyId == null) {
            // Fallback to value from DTO
            if (userCreationDTO.companyId() == null) {
                throw new ApiException("Company Id not provided");
            }
            companyId = UUID.fromString(userCreationDTO.companyId());
        }
        Optional<Tenant> tenant = tenantRepository.findById(companyId);
        if (tenant.isEmpty()) {
            throw new ApiException("Company does not exist with the provided id", userCreationDTO.companyId());
        }
        Role role = new Role();
        if (userCreationDTO.roleDetails().getName() == RoleEnum.SUPER_ADMIN.toString()) {
            List<Permission> permissions = permissionsRepository.getSuperAdminPermissions();
            role.setPermissions(new HashSet<>(permissions));
            role.setRoleName(userCreationDTO.roleDetails().getName());
            role.setTenantId(tenant.get().getId());
            roleRepository.save(role);
        } else{
           Optional<Role> optionalRole =  roleRepository.findById(userCreationDTO.roleDetails().getRoleId());
           if(optionalRole.isEmpty()){
            throw new ApiException("Invalid role assigned");
           }
           role = optionalRole.get();
        }
        user.setRole(role);
        user.setEmail(userCreationDTO.email());
        user.setPhoneNumber(userCreationDTO.phoneNumber());
        user.setTenant(tenant.get());
        user.setTenantId(tenant.get().getId());
        user.setName(userCreationDTO.name());

        String rawPassword = userCreationDTO.password();
        boolean isInviteFlow = (rawPassword == null || rawPassword.isBlank());

        if (isInviteFlow) {
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        } else {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

       User newUser = userRepository.save(user);

       if (isInviteFlow) {
           String token = otpService.generateActivationToken(newUser.getEmail());
           String activationLink = frontendUrl + Constants.INVITATION_ACCEPT_ENDPOINT + token;
           emailService.sendHtml(
               newUser.getEmail(),
               "You've been invited to Cognitia",
               "invitation",
               Map.of("activationLink", activationLink, "name", newUser.getName())
           );
           log.info("[UserServiceImpl] [createUser] Invitation email sent to {}", newUser.getEmail());
       }

       return Utils.mapToUserDetailsDTO(newUser);
    }

    @Override
    @Transactional
    public List<UserDetailsDTO> getAllUsers() {
        
       List<User> users = userRepository.findAll();

       List<UserDetailsDTO> userDetailsDTOs = users.stream().map(user -> {
            UserDetailsDTO  userDetailsDTO = new UserDetailsDTO();
            userDetailsDTO.setUserId(user.getId().toString());
            userDetailsDTO.setName(user.getName());
            userDetailsDTO.setEmail(user.getEmail());
            userDetailsDTO.setPhoneNumber(user.getPhoneNumber());
            userDetailsDTO.setCompanyId(user.getTenantId() != null ? user.getTenantId().toString() : null);
            userDetailsDTO.setRole(user.getRole() != null ? user.getRole().getRoleName() : null);
            return userDetailsDTO;
       }).collect(Collectors.toList());

       return userDetailsDTOs;
    }
}
