package com.intellidesk.cognitia.ingestion.models.entities;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.intellidesk.cognitia.ingestion.models.enums.Status;
import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "raw_resource", indexes = {
    @Index(name="idx_resource_id",columnList = "resId"),
    @Index(name="idx_resource_name", columnList = "name")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class RawSouce extends TenantAwareEntity{
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID resId;

    @Column(nullable = false, unique = true)
    private String assetId;


    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false, unique = true)
    private String secureUrl;

    @Column(nullable = false, unique = true)
    private String signature;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private String format;

    @Column(nullable = false)
    private Double size;

    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false )
    private Date createdAt;

    @LastModifiedDate
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, updatable = true )
    private Date updatedAt;
}
