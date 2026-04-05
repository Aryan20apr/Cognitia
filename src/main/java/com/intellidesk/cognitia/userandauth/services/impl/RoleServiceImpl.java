package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.userandauth.models.dtos.PermissionDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.RoleCreationDTO;
import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;
import com.intellidesk.cognitia.userandauth.models.entities.Permission;
import com.intellidesk.cognitia.userandauth.models.entities.Role;
import com.intellidesk.cognitia.userandauth.repository.ClassificationLevelRepository;
import com.intellidesk.cognitia.userandauth.repository.PermissionsRepository;
import com.intellidesk.cognitia.userandauth.repository.RoleRepository;
import com.intellidesk.cognitia.userandauth.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final ClassificationLevelRepository classificationLevelRepository;

    @Override
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

        role.setClearanceLevel(resolveClearanceLevel(roleCreationDTO.getClearanceLevelId()));

        Role newRole = roleRepository.save(role);
        roleCreationDTO.setRoleId(newRole.getRoleId());
        roleCreationDTO.setClearanceRank(deriveClearanceRank(newRole));
        return roleCreationDTO;
    }
    @Override
     @Transactional(readOnly = true)
     public List<RoleCreationDTO> getAllRoles() {
            List<Role> roles = roleRepository.findAll();
            return roles.stream().map(role -> {
                RoleCreationDTO dto = new RoleCreationDTO();
                dto.setRoleId(role.getRoleId());
                dto.setName(role.getRoleName());
                Set<PermissionDTO> perms = role.getPermissions().stream()
                    .map(p -> new PermissionDTO(p.getPermissionId(), p.getName()))
                    .collect(Collectors.toSet());
                dto.setPermissions(perms);
                dto.setClearanceLevelId(role.getClearanceLevel() != null ? role.getClearanceLevel().getId() : null);
                dto.setClearanceRank(deriveClearanceRank(role));
                return dto;
            }).collect(Collectors.toList());
        }

    @Override
    @Transactional
    public RoleCreationDTO updateRole(RoleCreationDTO roleCreationDTO){
        Role role = roleRepository.findById(roleCreationDTO.getRoleId()).orElseThrow(() -> new ApiException("Role not found"));
        
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
        
        role.setRoleName(roleCreationDTO.getName());
        role.setPermissions(new HashSet<>(existingPermissions));
        if (roleCreationDTO.getClearanceLevelId() != null) {
            role.setClearanceLevel(resolveClearanceLevel(roleCreationDTO.getClearanceLevelId()));
        }
        Role updatedRole = roleRepository.save(role);
        roleCreationDTO.setRoleId(updatedRole.getRoleId());
        roleCreationDTO.setClearanceRank(deriveClearanceRank(updatedRole));
        return roleCreationDTO;
    }

    private ClassificationLevel resolveClearanceLevel(UUID levelId) {
        if (levelId == null) {
            return null;
        }
        return classificationLevelRepository.findById(levelId)
            .orElseThrow(() -> new ApiException("Classification level not found: " + levelId));
    }

    private int deriveClearanceRank(Role role) {
        return (role.getClearanceLevel() != null && role.getClearanceLevel().getRank() != null)
            ? role.getClearanceLevel().getRank()
            : 0;
    }

    @Override
    @Transactional
    public void deleteRole(Integer roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException("Role not found"));

        if (userRepository.existsByRole_RoleId(roleId)) {
            throw new ApiException("Cannot delete role that is currently assigned to users");
        }

        roleRepository.delete(role);
    }
    
}