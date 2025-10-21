package com.intellidesk.cognitia.utils;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Permission;
import com.intellidesk.cognitia.userandauth.models.entities.User;

@Component
public class Utils {
    
    public static UserDetailsDTO mapToUserDetailsDTO(User user){
        // Extract role name
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        
        // Extract permission names - no additional DB calls needed since Role has EAGER fetch
        List<String> permissionNames = user.getRole() != null && user.getRole().getPermissions() != null
            ? user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .toList()
            : List.of();
        
        return new UserDetailsDTO(
            user.getId().toString(),
            user.getName(),
            user.getEmail(),
            user.getPhoneNumber(),
            roleName,
            user.getTenant() != null ? user.getTenant().getId().toString() : null,
            roleName, // This appears to be a duplicate field in the DTO
            permissionNames
        );
    }
}
