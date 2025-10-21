package com.intellidesk.cognitia.userandauth.models.dtos;

public record UserCreationDTO(String name, String password, String email, String companyId, String phoneNumber, RoleCreationDTO roleCreationDTO ) {
    
}
