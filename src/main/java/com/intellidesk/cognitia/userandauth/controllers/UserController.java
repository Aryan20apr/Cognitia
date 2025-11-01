package com.intellidesk.cognitia.userandauth.controllers;



import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cloudinary.http44.api.Response;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.UserCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.services.UserService;


import lombok.RequiredArgsConstructor;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping()
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserDetailsDTO>> createUser(@RequestBody UserCreationDTO userCreationDTO) {
        
        UserDetailsDTO userDetailsDTO =  userService.createUser(userCreationDTO);
        
        ApiResponse<UserDetailsDTO> apiResponse = new ApiResponse<>("User created successfully", true, userDetailsDTO);

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
        
    }
    @GetMapping()
    public ResponseEntity<ApiResponse<List<UserDetailsDTO>>> getAllUsers() {
        List<UserDetailsDTO> users = userService.getAllUsers();
        ApiResponse<List<UserDetailsDTO>> apiResponse = new ApiResponse<>(
                "Users fetched successfully", true, users);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
    
    
}
