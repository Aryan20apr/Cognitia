package com.intellidesk.cognitia.chat.models.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.utils.uuidv7.GeneratedUuidV7;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "chat_shares")
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class ChatShare extends TenantAwareEntity {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_thread_id", nullable = false)
    private ChatThread sourceThread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "token", nullable = false, unique = true, length = 22)
    private String token;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Column(name = "title")
    private String title;

    @Column(name = "snapshot_version", nullable = false)
    @Builder.Default
    private Integer snapshotVersion = 1;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private User revokedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "share", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatShareMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "share", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatShareGrant> grants = new ArrayList<>();
}
