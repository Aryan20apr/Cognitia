package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.userandauth.models.dtos.PermissionDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.RoleCreationDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Permission;
import com.intellidesk.cognitia.userandauth.models.entities.Role;
import com.intellidesk.cognitia.userandauth.repository.PermissionsRepository;
import com.intellidesk.cognitia.userandauth.repository.RoleRepository;
import com.intellidesk.cognitia.userandauth.services.RoleService;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService{
    
    private final RoleRepository roleRepository;
    private final PermissionsRepository permissionRepository;

    @Transactional
    public RoleCreationDTO createRole(RoleCreationDTO roleCreationDTO) {

        log.info("Creating role with permissions: {}", roleCreationDTO.getPermissions());


        Set<Integer> requestedIds = roleCreationDTO.getPermissions()
                .stream()
                .map(PermissionDTO::getId)
                .collect(Collectors.toSet());

        List<Permission> existingPermissions = permissionRepository.findAllById(requestedIds);

        if (existingPermissions.size() != requestedIds.size()) {
            Set<Integer> foundIds = existingPermissions.stream()
                    .map(Permission::getPermissionId)
                    .collect(Collectors.toSet());

            Set<Integer> missingIds = new HashSet<>(requestedIds);
            missingIds.removeAll(foundIds);

            throw new ApiException("Some permissions not found", missingIds.toString());
        }

        Role role = new Role();
        role.setRoleName(roleCreationDTO.getName());
        role.setPermissions(new HashSet<>(existingPermissions));

        Role newRole = roleRepository.save(role);
        roleCreationDTO.setRoleId(newRole.getRoleId());
        return roleCreationDTO;
    }
}