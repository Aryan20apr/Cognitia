package com.intellidesk.cognitia.chat.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.chat.models.dtos.ChatMessageDTO;
import com.intellidesk.cognitia.chat.models.dtos.ShareResponseDTO;
import com.intellidesk.cognitia.chat.models.dtos.SharedThreadViewDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatShare;
import com.intellidesk.cognitia.chat.models.entities.ChatShareGrant;
import com.intellidesk.cognitia.chat.models.entities.ChatShareMessage;
import com.intellidesk.cognitia.chat.repository.ChatShareGrantRepository;
import com.intellidesk.cognitia.chat.repository.ChatShareMessageRepository;
import com.intellidesk.cognitia.chat.repository.ChatShareRepository;
import com.intellidesk.cognitia.userandauth.models.entities.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareViewService {

    private final ChatShareRepository shareRepository;
    private final ChatShareMessageRepository messageRepository;
    private final ChatShareGrantRepository grantRepository;
    private final ShareAuthorizationService authorizationService;
    private final ShareTokenService tokenService;

    @Transactional(readOnly = true)
    public SharedThreadViewDTO viewByToken(String token, User viewer, UUID viewerTenantId) {
        ChatShare share = shareRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        Integer viewerRoleId = viewer.getRole() != null ? viewer.getRole().getRoleId() : null;
        boolean canView = authorizationService.canView(share, viewer.getId(), viewerTenantId, viewerRoleId);

        if (!canView) {
            log.warn("[ShareViewService] User {} denied access to share {}", viewer.getId(), share.getId());
            throw new RuntimeException("Access denied to this shared thread");
        }

        List<ChatShareMessage> messages = messageRepository
                .findAllByShare_IdAndSnapshotVersionOrderBySequenceNoAsc(share.getId(), share.getSnapshotVersion());

        List<ChatMessageDTO> messageDTOs = messages.stream()
                .map(msg -> ChatMessageDTO.builder()
                        .id(msg.getId())
                        .content(msg.getContent())
                        .role(msg.getSender().name())
                        .createdAt(msg.getOriginalCreatedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("[ShareViewService] User {} accessed share {}", viewer.getId(), share.getId());

        return SharedThreadViewDTO.builder()
                .shareId(share.getId())
                .title(share.getTitle())
                .ownerName(share.getOwner().getName())
                .createdAt(share.getCreatedAt())
                .messages(messageDTOs)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ShareResponseDTO> getSharedWithMe(UUID userId, Integer userRoleId) {
        List<ChatShareGrant> userGrants = grantRepository.findAllByGranteeUser_Id(userId);

        List<ChatShare> shares = userGrants.stream()
                .map(ChatShareGrant::getShare)
                .collect(Collectors.toList());

        if (userRoleId != null) {
            List<ChatShareGrant> roleGrants = grantRepository.findAllByGranteeRole_RoleId(userRoleId);
            roleGrants.stream()
                    .map(ChatShareGrant::getShare)
                    .forEach(shares::add);
        }

        return shares.stream()
                .distinct()
                .filter(share -> share.getRevokedAt() == null)
                .map(share -> mapToResponseDTO(share))
                .collect(Collectors.toList());
    }

    private ShareResponseDTO mapToResponseDTO(ChatShare share) {
        return ShareResponseDTO.builder()
                .id(share.getId())
                .token(share.getToken())
                .url(tokenService.buildUrl(share.getToken()))
                .isPublic(share.isPublic())
                .title(share.getTitle())
                .snapshotVersion(share.getSnapshotVersion())
                .expiresAt(share.getExpiresAt())
                .createdAt(share.getCreatedAt())
                .sourceThreadId(share.getSourceThread().getId())
                .ownerUserId(share.getOwner().getId())
                .build();
    }
}
