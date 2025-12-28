package com.intellidesk.cognitia.ingestion.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.ingestion.models.dtos.IngestionJobDetails;
import com.intellidesk.cognitia.ingestion.service.IngestionJobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/ingestion-job")
@Tag(name = "Resource Processing Jobs", description = "Resource Processing Jobs Management")
public class IngestionJobController {


    private IngestionJobService ingestionJobService;

    public IngestionJobController(IngestionJobService ingestionJobService) {
        this.ingestionJobService = ingestionJobService;
    }


    @GetMapping("/")
    @Operation(summary = "Get all resource ingestion jobs with pagination")
    public ResponseEntity<?> getAllIngestionJobs(@RequestParam(defaultValue = "0") int page,
                                                                                      @RequestParam(defaultValue = "10") int size) {
        Page<IngestionJobDetails> jobs = ingestionJobService.getIngestionJobDetails(page, size);
        ApiResponse<Page<IngestionJobDetails>> response = new ApiResponse<>("Ingestion jobs fetched", true, jobs);
        return ResponseEntity.ok(response);
    } 
    
    
}
