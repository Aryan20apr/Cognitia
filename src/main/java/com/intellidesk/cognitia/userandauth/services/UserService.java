package com.intellidesk.cognitia.userandauth.services;

import java.util.List;

import java.util.UUID;

import com.intellidesk.cognitia.userandauth.models.dtos.UserCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserUpdateDTO;

public interface UserService {
    
    public UserDetailsDTO createUser(UserCreationDTO userCreationDTO);
    public List<UserDetailsDTO> getAllUsers();
    public UserDetailsDTO updateSelf(UUID userId, UserUpdateDTO userUpdateDTO);
    public void deleteUser(UUID userId);
    public UserDetailsDTO assignRole(UUID userId, Integer roleId);
    public UserDetailsDTO assignDepartments(UUID userId, List<UUID> departmentIds);
}
