package com.intellidesk.cognitia.analytics.controllers;


import java.util.List;

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
    @Operation(summary = "Get chat usage data", description = "Get chat usage optionally based on user id or thread id")
    public ResponseEntity<ApiResponse<List<ChatUsageDetailsDTO>>> getChatUsageData(@RequestParam(required = false) String userId, @RequestParam(required = false) String threadId) {

        List<ChatUsageDetailsDTO> chatUsageData = chatUsageService.getChatUsageData(userId, threadId);
        return ResponseEntity.ok(ApiResponse.<List<ChatUsageDetailsDTO>>builder()
                .message("Chat usage data fetched successfully")
                .success(true)
                .data(chatUsageData)
                .build());

    }
    
}
