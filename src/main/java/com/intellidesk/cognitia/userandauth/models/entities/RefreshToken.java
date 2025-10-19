package com.intellidesk.cognitia.userandauth.models.entities;

import java.util.Date;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    private String userAgent;

    private Date createdAt;
    private Date updatedAt;
    private Boolean revoked;
    private UUID replacedBy;
    private Date lastUsedAt;

    
}
