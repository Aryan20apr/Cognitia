package com.intellidesk.cognitia.userandauth.models.entities;

import java.util.Date;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer permissionId;

    private String name;

    @CreationTimestamp
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Date createdAt;

    @UpdateTimestamp
    @LastModifiedDate
    private Date updatedAt;

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Permission other = (Permission) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    
}
