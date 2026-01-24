package com.intellidesk.cognitia.analytics.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.analytics.models.dto.PlanDTO;
import com.intellidesk.cognitia.analytics.service.PlanCatalogService;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Plan catalog management")
public class PlanController {
    
    private final PlanCatalogService planCatalogService;

    @Operation(summary = "List all available plans")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plans retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanDTO>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.<List<PlanDTO>>builder()
                .data(planCatalogService.listPlans())
                .message("Plans retrieved successfully")
                .success(true)
                .build());
    }

    @Operation(summary = "Get plan by ID")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plan found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Plan not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanDTO>> getPlanById(
            @Parameter(description = "Plan ID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<PlanDTO>builder()
                .data(planCatalogService.findById(id))
                .message("Plan retrieved successfully")
                .success(true)
                .build());
    }

    @Operation(summary = "Get plan by code")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plan found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Plan not found")
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<PlanDTO>> getPlanByCode(
            @Parameter(description = "Plan code", required = true)
            @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.<PlanDTO>builder()
                .data(planCatalogService.findByCode(code))
                .message("Plan retrieved successfully")
                .success(true)
                .build());
    }

    @Operation(summary = "Create a new plan")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plan created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PLAN_CREATE')")
    public ResponseEntity<ApiResponse<PlanDTO>> createPlan(
            @RequestBody PlanDTO planDTO) {
        return ResponseEntity.ok(ApiResponse.<PlanDTO>builder()
                .data(planCatalogService.createPlan(planDTO))
                .message("Plan created successfully")
                .success(true)
                .build());
    }

    @Operation(summary = "Update an existing plan")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plan updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Plan not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PLAN_UPDATE')")
    public ResponseEntity<ApiResponse<PlanDTO>> updatePlan(
            @Parameter(description = "Plan ID", required = true)
            @PathVariable UUID id,
            @RequestBody PlanDTO planDTO) {
        return ResponseEntity.ok(ApiResponse.<PlanDTO>builder()
                .data(planCatalogService.updatePlan(id, planDTO))
                .message("Plan updated successfully")
                .success(true)
                .build());
    }

    @Operation(summary = "Deactivate a plan")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Plan deactivated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Plan not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PLAN_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> deactivatePlan(
            @Parameter(description = "Plan ID", required = true)
            @PathVariable UUID id) {
        planCatalogService.deactivatePlan(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Plan deactivated successfully")
                .success(true)
                .build());
    }
}
