package com.intellidesk.cognitia.chat.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.chat.models.dtos.CreateShareRequestDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareGrantDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareResponseDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareRoleSummaryDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareUserSummaryDTO;
import com.intellidesk.cognitia.chat.service.ShareSnapshotService;
import com.intellidesk.cognitia.chat.service.ShareSnapshotService.UpsertResult;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Share", description = "Chat thread sharing endpoints")
@PreAuthorize("hasAuthority('PERM_CHAT_SHARE')")
public class ChatShareController {

    private final ShareSnapshotService snapshotService;

    /**
     * Creates or updates the single share for this thread.
     * Returns 201 on first creation, 200 on subsequent updates.
     * The token (and therefore the URL) is preserved on updates.
     */
    @Operation(summary = "Create or update the share for a thread")
    @PostMapping("/threads/{threadId}/shares")
    public ResponseEntity<ApiResponse<ShareResponseDTO>> createOrUpdateShare(
            @PathVariable String threadId,
            @RequestBody CreateShareRequestDTO request) {

        UUID currentUserId = getCurrentUserId();
        UpsertResult<ShareResponseDTO> result = snapshotService.createOrUpdateShare(
                UUID.fromString(threadId), request, currentUserId);

        log.info("[ChatShareController] User {} {} share {} for thread {}",
                currentUserId,
                result.created() ? "created" : "updated",
                result.data().getId(),
                threadId);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created() ? "Share created successfully" : "Share updated successfully";
        return ResponseEntity.status(status).body(new ApiResponse<>(message, true, result.data()));
    }

    @Operation(summary = "Get the share for a thread, if one exists")
    @GetMapping("/threads/{threadId}/shares")
    public ResponseEntity<ApiResponse<ShareResponseDTO>> getShareForThread(@PathVariable String threadId) {
        UUID currentUserId = getCurrentUserId();
        Optional<ShareResponseDTO> share = snapshotService.getShareForThread(
                UUID.fromString(threadId), currentUserId);

        return share
                .map(dto -> ResponseEntity.ok(new ApiResponse<>("Share retrieved successfully", true, dto)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("No share found for this thread", false, null)));
    }

    @Operation(summary = "Get all shares owned by the current user")
    @GetMapping("/shares")
    public ResponseEntity<ApiResponse<List<ShareResponseDTO>>> getMyShares() {
        List<ShareResponseDTO> shares = snapshotService.getSharesForOwner(getCurrentUserId());
        return ResponseEntity.ok(new ApiResponse<>("Shares retrieved successfully", true, shares));
    }

    @Operation(summary = "Get current grants for a share")
    @GetMapping("/shares/{shareId}/grants")
    public ResponseEntity<ApiResponse<ShareGrantDTO>> getGrants(@PathVariable String shareId) {
        ShareGrantDTO grants = snapshotService.getGrantsForShare(UUID.fromString(shareId));
        return ResponseEntity.ok(new ApiResponse<>("Grants retrieved successfully", true, grants));
    }

    @Operation(summary = "List all users in the tenant — used to populate the share user picker")
    @GetMapping("/shares/lookup/users")
    @PreAuthorize("hasAuthority('PERM_CHAT_ACCESS')")
    public ResponseEntity<ApiResponse<List<ShareUserSummaryDTO>>> getTenantUsers() {
        List<ShareUserSummaryDTO> users = snapshotService.getTenantUsers(TenantContext.getTenantId());
        return ResponseEntity.ok(new ApiResponse<>("Users retrieved successfully", true, users));
    }

    @Operation(summary = "List all roles in the tenant — used to populate the share role picker")
    @GetMapping("/shares/lookup/roles")
    @PreAuthorize("hasAuthority('PERM_CHAT_ACCESS')")
    public ResponseEntity<ApiResponse<List<ShareRoleSummaryDTO>>> getTenantRoles() {
        List<ShareRoleSummaryDTO> roles = snapshotService.getTenantRoles(TenantContext.getTenantId());
        return ResponseEntity.ok(new ApiResponse<>("Roles retrieved successfully", true, roles));
    }

    @Operation(summary = "Revoke a share")
    @PostMapping("/shares/{shareId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeShare(@PathVariable String shareId) {
        UUID currentUserId = getCurrentUserId();
        snapshotService.revokeShare(UUID.fromString(shareId), currentUserId);

        log.info("[ChatShareController] User {} revoked share {}", currentUserId, shareId);

        return ResponseEntity.ok(new ApiResponse<>("Share revoked successfully", true, null));
    }

    private UUID getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUser().getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
