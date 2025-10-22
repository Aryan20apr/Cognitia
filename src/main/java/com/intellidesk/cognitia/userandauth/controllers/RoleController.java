package com.intellidesk.cognitia.userandauth.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.RoleCreationDTO;
import com.intellidesk.cognitia.userandauth.services.RoleService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("api/v1/role")
@RequiredArgsConstructor
public class RoleController {
    

    private final RoleService roleService;

    @PostMapping()
    public ResponseEntity<ApiResponse<?>> postMethodName(@RequestBody RoleCreationDTO roleCreationDTO) {
        
        RoleCreationDTO roleCreationDTO2 = roleService.createRole(roleCreationDTO);
        return new ResponseEntity<>(new ApiResponse<>("Role created successfully",true, roleCreationDTO2), HttpStatus.CREATED);
    }
    
}
