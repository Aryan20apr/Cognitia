package com.intellidesk.cognitia.userandauth.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.RoleCreationDTO;
import com.intellidesk.cognitia.userandauth.services.RoleService;

import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("api/v1/role")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role management")
public class RoleController {
    

    private final RoleService roleService;

    @Operation(summary = "Create a new role with permissions")
    @PostMapping()
    public ResponseEntity<ApiResponse<?>> postMethodName(@RequestBody RoleCreationDTO roleCreationDTO) {
        
        RoleCreationDTO roleCreationDTO2 = roleService.createRole(roleCreationDTO);
        return new ResponseEntity<>(new ApiResponse<>("Role created successfully",true, roleCreationDTO2), HttpStatus.CREATED);
    }

    @Operation(summary = "Get all roles")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getRoles() {
        List<RoleCreationDTO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(new ApiResponse<>("Roles fetched successfully", true, roles));
    }
    
}
