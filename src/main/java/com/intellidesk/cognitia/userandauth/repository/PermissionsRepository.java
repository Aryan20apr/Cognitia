package com.intellidesk.cognitia.userandauth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.intellidesk.cognitia.userandauth.models.entities.Permission;

public interface PermissionsRepository extends JpaRepository<Permission, Integer>{
    
    Optional<Permission> findByName(String name);

    @Query(value = "SELECT * FROM permission p WHERE p.name <> 'TENANT_CREATE'", nativeQuery = true)
    List<Permission> getSuperAdminPermissions();
}
