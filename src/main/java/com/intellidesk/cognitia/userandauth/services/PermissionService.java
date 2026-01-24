package com.intellidesk.cognitia.userandauth.services;

import java.util.List;
import com.intellidesk.cognitia.userandauth.models.dtos.PermissionDTO;

public interface PermissionService {
    
    List<PermissionDTO> getAllPermissions();
}
