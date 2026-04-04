package com.intellidesk.cognitia.userandauth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;

public interface ClassificationLevelRepository extends JpaRepository<ClassificationLevel, UUID> {

    List<ClassificationLevel> findByTenantIdOrderByRankAsc(UUID tenantId);

    Optional<ClassificationLevel> findByTenantIdAndName(UUID tenantId, String name);

    Optional<ClassificationLevel> findByTenantIdAndRank(UUID tenantId, Integer rank);

    Optional<ClassificationLevel> findFirstByTenantIdOrderByRankAsc(UUID tenantId);

    Optional<ClassificationLevel> findFirstByTenantIdOrderByRankDesc(UUID tenantId);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndRank(UUID tenantId, Integer rank);
}
