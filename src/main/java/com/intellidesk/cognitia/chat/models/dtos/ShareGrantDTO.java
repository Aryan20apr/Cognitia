package com.intellidesk.cognitia.chat.models.dtos;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the current grants on a share, split by grant type.
 * User grants and role grants are independent lists — a single entry
 * never carries both a user and a role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareGrantDTO {
    private List<UserGrant> userGrants;
    private List<RoleGrant> roleGrants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGrant {
        private UUID grantId;
        private UUID userId;
        private String userName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleGrant {
        private UUID grantId;
        private Integer roleId;
        private String roleName;
    }
}
