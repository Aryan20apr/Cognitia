package com.intellidesk.cognitia.userandauth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.userandauth.models.entities.Department;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findByTenantId(UUID tenantId);

    Optional<Department> findByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndName(UUID tenantId, String name);
}
