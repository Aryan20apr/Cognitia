package com.intellidesk.cognitia.ingestion.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.ingestion.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceDetails;
import com.intellidesk.cognitia.ingestion.models.dtos.ResourceMetadata;
import com.intellidesk.cognitia.ingestion.service.ResourceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/resource")
@Tag(name = "Resources", description = "Raw resource ingestion and upload")
public class RawResourceController {

    private final ResourceService resourceService;

    @Operation(summary = "Upload and ingest raw resource (multipart)")
    @PostMapping(value = "/ingest",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ingestResource( @RequestPart("file") MultipartFile file,
        @RequestPart("name") String name, @RequestPart("description") String description) {
        
        ResourceMetadata metadata = new ResourceMetadata(name, description);
        CloudinaryUploadResult res = resourceService.uploadRawResource(file, metadata);
        ApiResponse<CloudinaryUploadResult> apiResponse = ApiResponse.<CloudinaryUploadResult>builder()
                .message("File uploaded sccessfully").data(res).success(true).build();
        return ResponseEntity.status(201).body(apiResponse);
    }
    

    @Operation(summary = "Get all uploaded resources")
    @GetMapping(value = "/history")
    public ResponseEntity<?> getResourceHistory() {
        List<ResourceDetails> history = resourceService.getResourceUploadHistory();
        return ResponseEntity.ok(new ApiResponse<>("History fetched", true, history));
    }


}
