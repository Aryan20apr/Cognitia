package com.intellidesk.cognitia.chat.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.chat.models.entities.ChatShare;
import com.intellidesk.cognitia.chat.repository.ChatShareGrantRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareAuthorizationService {

    private final ChatShareGrantRepository grantRepository;

    public boolean canView(ChatShare share, UUID viewerUserId, UUID viewerTenantId, Integer viewerRoleId) {
        if (share.getRevokedAt() != null) {
            log.debug("[ShareAuthorizationService] Access denied to share {} - revoked", share.getId());
            return false;
        }

        if (share.getExpiresAt() != null && Instant.now().isAfter(share.getExpiresAt())) {
            log.debug("[ShareAuthorizationService] Access denied to share {} - expired", share.getId());
            return false;
        }

        if (!viewerTenantId.equals(share.getTenantId())) {
            log.debug("[ShareAuthorizationService] Access denied to share {} - cross-tenant", share.getId());
            return false;
        }

        if (share.isPublic()) {
            log.debug("[ShareAuthorizationService] Access granted to share {} - public", share.getId());
            return true;
        }

        // Private share: viewer must have an explicit user grant or a matching role grant
        boolean hasUserGrant = grantRepository.existsByShare_IdAndGranteeUser_Id(share.getId(), viewerUserId);
        if (hasUserGrant) {
            log.debug("[ShareAuthorizationService] Access granted to share {} - user grant", share.getId());
            return true;
        }

        if (viewerRoleId != null) {
            boolean hasRoleGrant = grantRepository.existsByShare_IdAndGranteeRole_RoleId(share.getId(), viewerRoleId);
            if (hasRoleGrant) {
                log.debug("[ShareAuthorizationService] Access granted to share {} - role grant", share.getId());
                return true;
            }
        }

        log.debug("[ShareAuthorizationService] Access denied to share {} - no grant", share.getId());
        return false;
    }
}
