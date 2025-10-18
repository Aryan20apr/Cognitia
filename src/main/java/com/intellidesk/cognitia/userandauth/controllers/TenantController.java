package com.intellidesk.cognitia.userandauth.controllers;

import java.util.List;
import java.util.Set;

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
import com.intellidesk.cognitia.userandauth.services.TenantService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("public/api/v1/company")
public class TenantController {
    
    private TenantService tenantService;


    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> createCompany(@RequestBody TenantDTO tenantDTO){

       Tenant tenant = tenantService.createTenant(tenantDTO);
       ApiResponse<Tenant> apiResponse = new ApiResponse<>("Tenant created successfully", true, tenant);
       return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<Tenant>>> getAllCompanies(){

       List<Tenant> companies = tenantService.getAllCompanies();
       ApiResponse<List<Tenant>> apiResponse = new ApiResponse<>("Companies fetched", true, companies);
       return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
    
}
