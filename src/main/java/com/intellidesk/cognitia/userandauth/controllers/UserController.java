package com.intellidesk.cognitia.userandauth.controllers;



import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.UserCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user (Requires SUPER_ADMIN)")
    @PostMapping()
    @PreAuthorize("hasAuthority('PERM_USER_CREATE')")
    public ResponseEntity<ApiResponse<UserDetailsDTO>> createUser(@Valid @RequestBody UserCreationDTO userCreationDTO) {
        
        UserDetailsDTO userDetailsDTO =  userService.createUser(userCreationDTO);
        
        ApiResponse<UserDetailsDTO> apiResponse = new ApiResponse<>("User created successfully", true, userDetailsDTO);

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
        
    }
    @Operation(summary = "List all users")
    @GetMapping()
    @PreAuthorize("hasAuthority('PERM_USER_READ')")
    public ResponseEntity<ApiResponse<List<UserDetailsDTO>>> getAllUsers() {
        List<UserDetailsDTO> users = userService.getAllUsers();
        ApiResponse<List<UserDetailsDTO>> apiResponse = new ApiResponse<>(
                "Users fetched successfully", true, users);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
    
    
}
