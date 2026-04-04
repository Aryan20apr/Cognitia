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
import com.intellidesk.cognitia.userandauth.models.dtos.ClassificationLevelDTO;
import com.intellidesk.cognitia.userandauth.services.ClassificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/classifications")
@RequiredArgsConstructor
@Tag(name = "Classifications", description = "Classification level management")
public class ClassificationController {

    private final ClassificationService classificationService;

    @Operation(summary = "Create a new classification level")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CLASSIFICATION_MANAGE')")
    public ResponseEntity<ApiResponse<ClassificationLevelDTO>> create(@RequestBody ClassificationLevelDTO dto) {
        ClassificationLevelDTO created = classificationService.create(dto.name(), dto.rank());
        return new ResponseEntity<>(new ApiResponse<>("Classification level created", true, created), HttpStatus.CREATED);
    }

    @Operation(summary = "List all classification levels for this tenant (ordered by rank)")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CLASSIFICATION_MANAGE')")
    public ResponseEntity<ApiResponse<List<ClassificationLevelDTO>>> getAll() {
        List<ClassificationLevelDTO> levels = classificationService.getAll();
        return ResponseEntity.ok(new ApiResponse<>("Classification levels fetched", true, levels));
    }

    @Operation(summary = "Update a classification level's name or rank")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CLASSIFICATION_MANAGE')")
    public ResponseEntity<ApiResponse<ClassificationLevelDTO>> update(@PathVariable UUID id,
                                                                       @RequestBody ClassificationLevelDTO dto) {
        ClassificationLevelDTO updated = classificationService.update(id, dto.name(), dto.rank());
        return ResponseEntity.ok(new ApiResponse<>("Classification level updated", true, updated));
    }

    @Operation(summary = "Delete a classification level")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CLASSIFICATION_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        classificationService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>("Classification level deleted", true, null));
    }
}
