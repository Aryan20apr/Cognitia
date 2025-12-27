package com.intellidesk.cognitia.userandauth.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.TenantDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Tenant;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.services.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/company")
@Tag(name = "Tenants", description = "Tenant management endpoints")
public class TenantController {
    
    private TenantService tenantService;


    @Operation(summary = "Create tenant / company")
    @PostMapping
    public ResponseEntity<ApiResponse<TenantDTO>> createCompany(@RequestBody TenantDTO tenantDTO){

       TenantDTO tenant = tenantService.createTenant(tenantDTO);
       ApiResponse<TenantDTO> apiResponse = new ApiResponse<>("Tenant created successfully", true, tenant);
       return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @Operation(description = "Get tenant by id")
    @GetMapping("/details")
    public ResponseEntity<ApiResponse<TenantDTO>> getTenantById(){
        TenantDTO tenant = tenantService.getTenant(TenantContext.getTenantId().toString());
        ApiResponse<TenantDTO> apiResponse = new ApiResponse<>("Tenant fetched successfully", true, tenant);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }


    @Operation(summary = "List all tenants")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Tenant>>> getAllCompanies(){

       List<Tenant> companies = tenantService.getAllCompanies();
       ApiResponse<List<Tenant>> apiResponse = new ApiResponse<>("Companies fetched", true, companies);
       return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
    
}
