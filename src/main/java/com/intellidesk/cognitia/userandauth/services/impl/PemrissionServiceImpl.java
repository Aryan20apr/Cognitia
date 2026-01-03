package com.intellidesk.cognitia.userandauth.services.impl;


import java.util.List;

import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.userandauth.models.dtos.PermissionDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Permission;
import com.intellidesk.cognitia.userandauth.repository.PermissionsRepository;
import com.intellidesk.cognitia.userandauth.services.PermissionService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class PemrissionServiceImpl implements PermissionService {
    
    private final PermissionsRepository permissionRepository;

    @Override
    public List<PermissionDTO> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream().map(permission -> new PermissionDTO(permission.getPermissionId(), permission.getName())).toList();
    }
}
