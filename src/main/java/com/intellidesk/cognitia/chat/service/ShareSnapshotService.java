package com.intellidesk.cognitia.chat.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.chat.models.dtos.CreateShareRequestDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareGrantDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareResponseDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareRoleSummaryDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareUserSummaryDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatMessage;
import com.intellidesk.cognitia.chat.models.entities.ChatShare;
import com.intellidesk.cognitia.chat.models.entities.ChatShareGrant;
import com.intellidesk.cognitia.chat.models.entities.ChatShareMessage;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.repository.ChatShareGrantRepository;
import com.intellidesk.cognitia.chat.repository.ChatShareMessageRepository;
import com.intellidesk.cognitia.chat.repository.ChatShareRepository;
import com.intellidesk.cognitia.chat.repository.ChatThreadRepository;
import com.intellidesk.cognitia.userandauth.repository.RoleRepository;
import com.intellidesk.cognitia.userandauth.repository.UserRepository;
import com.intellidesk.cognitia.chat.service.ThreadLockService.ThreadLockStatus;
import com.intellidesk.cognitia.userandauth.models.entities.Role;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.utils.exceptionHandling.ThreadBusyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareSnapshotService {

    private final ChatThreadRepository threadRepository;
    private final ChatShareRepository shareRepository;
    private final ChatShareMessageRepository shareMessageRepository;
    private final ChatShareGrantRepository grantRepository;
    private final ThreadLockService threadLockService;
    private final ShareTokenService tokenService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Creates or updates the single share for this thread (upsert).
     *
     * On first call: inserts a new share row with a fresh token and snapshot version 1.
     * On subsequent calls: updates isPublic, expiry, grants, and snapshot in place.
     * The token is preserved so previously distributed URLs continue to work.
     *
     * Returns UpsertResult so the controller can distinguish 201 vs 200.
     */
    @Transactional
    public UpsertResult<ShareResponseDTO> createOrUpdateShare(UUID threadId, CreateShareRequestDTO request, UUID ownerId) {
        String lockToken = threadLockService.tryAcquire(threadId);
        if (lockToken == null) {
            ThreadLockStatus status = threadLockService.getStatus(threadId);
            throw new ThreadBusyException(threadId.toString(), status.getWaitingCount());
        }

        try {
            ChatThread thread = threadRepository.findByIdAndUserId(threadId, ownerId)
                    .orElseThrow(() -> new RuntimeException("Thread not found or not owned by user"));

            Optional<ChatShare> existing = shareRepository.findBySourceThread_IdAndOwner_Id(threadId, ownerId);

            if (existing.isPresent()) {
                ChatShare share = existing.get();

                if (share.getRevokedAt() != null) {
                    throw new RuntimeException("This thread's share has been revoked and cannot be updated");
                }

                int newVersion = share.getSnapshotVersion() + 1;
                share.setSnapshotVersion(newVersion);
                share.setTitle(thread.getTitle());
                share.setExpiresAt(request.getExpiresAt());
                share.setPublic(request.isPublic());

                shareMessageRepository.saveAll(createSnapshotMessages(thread, share, newVersion));
                replaceGrants(share, request, ownerId);
                shareRepository.save(share);

                log.info("[ShareSnapshotService] Updated share {} for thread {}", share.getId(), threadId);
                return new UpsertResult<>(mapToResponseDTO(share, share.getToken()), false);

            } else {
                String token = tokenService.generateUniqueToken(t -> {
                    shareRepository.findByToken(t).ifPresent(s -> {
                        throw new RuntimeException("Token collision");
                    });
                    return true;
                });

                ChatShare share = ChatShare.builder()
                        .sourceThread(thread)
                        .owner(createUserReference(ownerId))
                        .token(token)
                        .isPublic(request.isPublic())
                        .title(thread.getTitle())
                        .snapshotVersion(1)
                        .expiresAt(request.getExpiresAt())
                        .build();
                share.setTenantId(thread.getTenantId());
                share = shareRepository.save(share);

                shareMessageRepository.saveAll(createSnapshotMessages(thread, share, 1));
                applyGrants(share, request, ownerId);

                log.info("[ShareSnapshotService] Created share {} for thread {}", share.getId(), threadId);
                return new UpsertResult<>(mapToResponseDTO(share, token), true);
            }

        } finally {
            threadLockService.release(threadId, lockToken);
        }
    }

    public record UpsertResult<T>(T data, boolean created) {}

    @Transactional
    public void revokeShare(UUID shareId, UUID requestingUserId) {
        ChatShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        if (!share.getOwner().getId().equals(requestingUserId)) {
            throw new RuntimeException("Only the owner can revoke a share");
        }

        share.setRevokedAt(Instant.now());
        share.setRevokedBy(createUserReference(requestingUserId));

        shareRepository.save(share);

        log.info("[ShareSnapshotService] Revoked share {}", shareId);
    }

    @Transactional(readOnly = true)
    public List<ShareUserSummaryDTO> getTenantUsers(UUID tenantId) {
        return userRepository.findSummariesByTenantId(tenantId).stream()
                .map(u -> ShareUserSummaryDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShareRoleSummaryDTO> getTenantRoles(UUID tenantId) {
        return roleRepository.findSummariesByTenantId(tenantId).stream()
                .map(r -> ShareRoleSummaryDTO.builder()
                        .id(r.getRoleId())
                        .name(r.getRoleName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShareGrantDTO getGrantsForShare(UUID shareId) {
        List<ChatShareGrant> grants = grantRepository.findAllByShare_Id(shareId);

        List<ShareGrantDTO.UserGrant> userGrants = grants.stream()
                .filter(g -> g.getGranteeUser() != null)
                .map(g -> ShareGrantDTO.UserGrant.builder()
                        .grantId(g.getId())
                        .userId(g.getGranteeUser().getId())
                        .userName(g.getGranteeUser().getName())
                        .build())
                .collect(Collectors.toList());

        List<ShareGrantDTO.RoleGrant> roleGrants = grants.stream()
                .filter(g -> g.getGranteeRole() != null)
                .map(g -> ShareGrantDTO.RoleGrant.builder()
                        .grantId(g.getId())
                        .roleId(g.getGranteeRole().getRoleId())
                        .roleName(g.getGranteeRole().getRoleName())
                        .build())
                .collect(Collectors.toList());

        return ShareGrantDTO.builder()
                .userGrants(userGrants)
                .roleGrants(roleGrants)
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<ShareResponseDTO> getShareForThread(UUID threadId, UUID requestingUserId) {
        return shareRepository.findBySourceThread_IdAndOwner_Id(threadId, requestingUserId)
                .map(share -> mapToResponseDTO(share, share.getToken()));
    }

    @Transactional(readOnly = true)
    public List<ShareResponseDTO> getSharesForOwner(UUID ownerId) {
        return shareRepository.findAllByOwner_Id(ownerId).stream()
                .map(share -> mapToResponseDTO(share, share.getToken()))
                .collect(Collectors.toList());
    }

    private List<ChatShareMessage> createSnapshotMessages(ChatThread thread, ChatShare share, int version) {
        List<ChatMessage> messages = thread.getMessages();
        List<ChatShareMessage> snapshotMessages = new ArrayList<>();

        int sequenceNo = 0;
        for (ChatMessage message : messages) {
            if (message.getSender() == MessageType.USER || message.getSender() == MessageType.ASSISTANT) {
                snapshotMessages.add(ChatShareMessage.builder()
                        .share(share)
                        .snapshotVersion(version)
                        .sequenceNo(sequenceNo++)
                        .sender(message.getSender())
                        .content(message.getContent())
                        .originalMessageId(message.getId())
                        .originalCreatedAt(message.getCreatedAt().toInstant())
                        .build());
            }
        }

        return snapshotMessages;
    }

    private void replaceGrants(ChatShare share, CreateShareRequestDTO request, UUID grantedBy) {
        List<ChatShareGrant> existing = grantRepository.findAllByShare_Id(share.getId());
        if (!existing.isEmpty()) {
            grantRepository.deleteAll(existing);
            grantRepository.flush();
        }
        applyGrants(share, request, grantedBy);
    }

    private void applyGrants(ChatShare share, CreateShareRequestDTO request, UUID grantedBy) {
        List<ChatShareGrant> grants = new ArrayList<>();

        if (request.getGranteeUserIds() != null) {
            for (UUID userId : request.getGranteeUserIds()) {
                grants.add(ChatShareGrant.builder()
                        .share(share)
                        .granteeUser(createUserReference(userId))
                        .grantedBy(createUserReference(grantedBy))
                        .build());
            }
        }

        if (request.getGranteeRoleIds() != null) {
            for (Integer roleId : request.getGranteeRoleIds()) {
                grants.add(ChatShareGrant.builder()
                        .share(share)
                        .granteeRole(createRoleReference(roleId))
                        .grantedBy(createUserReference(grantedBy))
                        .build());
            }
        }

        if (!grants.isEmpty()) {
            grantRepository.saveAll(grants);
        }
    }

    private ShareResponseDTO mapToResponseDTO(ChatShare share, String token) {
        return ShareResponseDTO.builder()
                .id(share.getId())
                .token(token)
                .url(tokenService.buildUrl(token))
            
                .isPublic(share.isPublic())
                .title(share.getTitle())
                .snapshotVersion(share.getSnapshotVersion())
                .expiresAt(share.getExpiresAt())
                .createdAt(share.getCreatedAt())
                .sourceThreadId(share.getSourceThread().getId())
                .ownerUserId(share.getOwner().getId())
                .build();
    }

    private User createUserReference(UUID userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Role createRoleReference(Integer roleId) {
        Role role = new Role();
        role.setRoleId(roleId);
        return role;
    }
}
