package com.intellidesk.cognitia.chat.models.entities;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.intellidesk.cognitia.userandauth.models.entities.Role;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.utils.uuidv7.GeneratedUuidV7;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_share_grants")
public class ChatShareGrant {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id", nullable = false)
    private ChatShare share;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_user_id")
    private User granteeUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grantee_role_id")
    private Role granteeRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;
}
