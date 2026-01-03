package com.intellidesk.cognitia.userandauth.services;

import java.util.List;

import com.intellidesk.cognitia.userandauth.models.dtos.RoleCreationDTO;

public interface RoleService {

    List<RoleCreationDTO> getAllRoles();

    public RoleCreationDTO createRole(RoleCreationDTO roleCreationDTO);

    public RoleCreationDTO updateRole(RoleCreationDTO roleCreationDTO);

} 
    
