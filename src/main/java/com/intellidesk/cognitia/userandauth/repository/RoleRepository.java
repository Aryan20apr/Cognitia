package com.intellidesk.cognitia.userandauth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.intellidesk.cognitia.userandauth.models.entities.Role;
import com.intellidesk.cognitia.userandauth.repository.projections.RoleSummary;

public interface RoleRepository extends JpaRepository<Role,Integer> {
    
    Optional<Role> findByRoleName(String roleName);

    @Query("SELECT r.roleId AS roleId, r.roleName AS roleName FROM Role r WHERE r.tenantId = :tenantId")
    List<RoleSummary> findSummariesByTenantId(@Param("tenantId") UUID tenantId);
}
