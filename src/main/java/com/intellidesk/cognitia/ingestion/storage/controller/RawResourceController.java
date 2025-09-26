package com.intellidesk.cognitia.ingestion.storage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.intellidesk.cognitia.ingestion.storage.models.dtos.ApiResponse;
import com.intellidesk.cognitia.ingestion.storage.models.dtos.CloudinaryUploadResult;
import com.intellidesk.cognitia.ingestion.storage.service.StorageService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/resource")
public class RawResourceController {

    private StorageService storageService;

    @PostMapping("/ingest")
    public ResponseEntity<?> ingestResource(@RequestBody MultipartFile file) {

        CloudinaryUploadResult res = storageService.uploadRawResource(file);
        ApiResponse<CloudinaryUploadResult> apiResponse = ApiResponse.<CloudinaryUploadResult>builder()
                .message("File uploaded sccessfully").data(res).success(true).build();
        return ResponseEntity.status(201).body(apiResponse);
    }

}
