package com.intellidesk.cognitia.ingestion.storage.models.entities;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.intellidesk.cognitia.ingestion.storage.models.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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

public class RawSouce {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID resId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false)
    private String language;

    private Status status;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Double size;

    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false )
    private Date createdAt;

    @LastModifiedDate
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, updatable = false )
    private Date updatedAt;

    @Override
    public int hashCode() {
       return Objects.hash(resId, name, description, url, language);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RawSouce other = (RawSouce) obj;
        if (resId == null) {
            if (other.resId != null)
                return false;
        } else if (!resId.equals(other.resId))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (language == null) {
            if (other.language != null)
                return false;
        } else if (!language.equals(other.language))
            return false;
        return true;
    }
}
