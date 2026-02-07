package com.intellidesk.cognitia.analytics.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.service.ChatUsageService;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat-usage")
@RequiredArgsConstructor
@Tag(name = "Chat Usage", description = "Chat usage endpoints")
public class ChatUsageController {

    private final ChatUsageService chatUsageService;

    @GetMapping()
    @Operation(
        summary = "Get chat usage data with pagination", 
        description = "Get paginated chat usage data optionally filtered by user id or thread id. Results are sorted by creation date descending by default."
    )
    public ResponseEntity<ApiResponse<Page<ChatUsageDetailsDTO>>> getChatUsageData(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String threadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        // Parse sort parameter
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        
        Page<ChatUsageDetailsDTO> chatUsageData = chatUsageService.getChatUsageData(userId, threadId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ChatUsageDetailsDTO>>builder()
                .message("Chat usage data fetched successfully")
                .success(true)
                .data(chatUsageData)
                .build());
    }
    
}
