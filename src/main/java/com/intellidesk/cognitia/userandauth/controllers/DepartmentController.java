package com.intellidesk.cognitia.userandauth.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.DepartmentDTO;
import com.intellidesk.cognitia.userandauth.services.DepartmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department management")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Create a new department")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> create(@RequestBody DepartmentDTO dto) {
        DepartmentDTO created = departmentService.create(dto.name());
        return new ResponseEntity<>(new ApiResponse<>("Department created", true, created), HttpStatus.CREATED);
    }

    @Operation(summary = "List all departments for this tenant")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAll() {
        List<DepartmentDTO> departments = departmentService.getAll();
        return ResponseEntity.ok(new ApiResponse<>("Departments fetched", true, departments));
    }

    @Operation(summary = "Rename a department")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentDTO>> update(@PathVariable UUID id, @RequestBody DepartmentDTO dto) {
        DepartmentDTO updated = departmentService.update(id, dto.name());
        return ResponseEntity.ok(new ApiResponse<>("Department updated", true, updated));
    }

    @Operation(summary = "Delete a department")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DEPARTMENT_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Department deleted", true, null));
    }
}
