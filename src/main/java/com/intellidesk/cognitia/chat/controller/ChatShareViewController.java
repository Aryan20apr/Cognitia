package com.intellidesk.cognitia.chat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.chat.models.dtos.ShareResponseDTO;
import com.intellidesk.cognitia.chat.models.dtos.SharedThreadViewDTO;
import com.intellidesk.cognitia.chat.service.ShareViewService;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat/shares/view")
@RequiredArgsConstructor
@Tag(name = "Chat Share View", description = "View shared chat threads")
@PreAuthorize("hasAuthority('PERM_CHAT_ACCESS')")
public class ChatShareViewController {

    private final ShareViewService shareViewService;

    @Operation(summary = "View a shared thread by token")
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<SharedThreadViewDTO>> viewSharedThread(@PathVariable String token) {
        User currentUser = getCurrentUser();
        SharedThreadViewDTO view = shareViewService.viewByToken(token, currentUser, TenantContext.getTenantId());
        return ResponseEntity.ok(new ApiResponse<>("Shared thread retrieved successfully", true, view));
    }

    @Operation(summary = "Get all threads shared with the current user")
    @GetMapping("/shared-with-me")
    public ResponseEntity<ApiResponse<List<ShareResponseDTO>>> getSharedWithMe() {
        User currentUser = getCurrentUser();
        Integer userRoleId = currentUser.getRole() != null ? currentUser.getRole().getRoleId() : null;
        List<ShareResponseDTO> shares = shareViewService.getSharedWithMe(currentUser.getId(), userRoleId);
        return ResponseEntity.ok(new ApiResponse<>("Shared threads retrieved successfully", true, shares));
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUser();
        }
        throw new RuntimeException("User not authenticated");
    }
}
