package com.intellidesk.cognitia.userandauth.services;

import com.intellidesk.cognitia.userandauth.models.dtos.RoleCreationDTO;

import java.util.List;

public interface RoleService {

    List<RoleCreationDTO> getAllRoles();

    public RoleCreationDTO createRole(RoleCreationDTO roleCreationDTO);

} 
    
