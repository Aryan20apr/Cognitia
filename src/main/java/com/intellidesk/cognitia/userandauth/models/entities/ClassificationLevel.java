package com.intellidesk.cognitia.userandauth.models.entities;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "classification_levels", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"}),
    @UniqueConstraint(columnNames = {"tenant_id", "rank"})
})
@Builder
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationLevel extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @CreationTimestamp
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Date createdAt;

    @UpdateTimestamp
    @LastModifiedDate
    private Date updatedAt;

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name, this.rank);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClassificationLevel other = (ClassificationLevel) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }
}
