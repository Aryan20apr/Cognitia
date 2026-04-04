package com.intellidesk.cognitia.userandauth.controllers;



import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.UserCreationDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.UserUpdateDTO;
import com.intellidesk.cognitia.userandauth.security.CustomUserDetails;
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

    @Operation(summary = "Update own profile (name, phone)")
    @PatchMapping()
    @PreAuthorize("hasAuthority('PERM_USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserDetailsDTO>> updateSelf(
            Authentication authentication,
            @Valid @RequestBody UserUpdateDTO userUpdateDTO) {
        UUID userId = ((CustomUserDetails) authentication.getPrincipal()).getUser().getId();
        UserDetailsDTO updated = userService.updateSelf(userId, userUpdateDTO);
        return ResponseEntity.ok(new ApiResponse<>("Profile updated successfully", true, updated));
    }

    @Operation(summary = "Delete a user by ID")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('PERM_USER_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(new ApiResponse<>("User deleted successfully", true, null));
    }

    @Operation(summary = "Assign a role to an existing user")
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('PERM_USER_ASSIGN_ROLE')")
    public ResponseEntity<ApiResponse<UserDetailsDTO>> assignRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, Integer> body) {
        Integer roleId = body.get("roleId");
        if (roleId == null) {
            throw new IllegalArgumentException("roleId is required");
        }
        UserDetailsDTO updated = userService.assignRole(userId, roleId);
        return ResponseEntity.ok(new ApiResponse<>("Role assigned successfully", true, updated));
    }

    @Operation(summary = "Assign departments to a user")
    @PatchMapping("/{userId}/departments")
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<UserDetailsDTO>> assignDepartments(
            @PathVariable UUID userId,
            @RequestBody List<UUID> departmentIds) {
        UserDetailsDTO updated = userService.assignDepartments(userId, departmentIds);
        return ResponseEntity.ok(new ApiResponse<>("Departments assigned successfully", true, updated));
    }
    
}
