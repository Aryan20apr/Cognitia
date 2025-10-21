package com.intellidesk.cognitia.userandauth.services;

import java.util.List;

import com.intellidesk.cognitia.userandauth.models.dtos.UserCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;

public interface UserService {
    
    public UserDetailsDTO createUser(UserCreationDTO userCreationDTO);
    public List<UserDetailsDTO> getAllUsers();
}
