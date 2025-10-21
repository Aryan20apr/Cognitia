package com.intellidesk.cognitia.userandauth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.userandauth.models.entities.Role;

public interface RoleRepository extends JpaRepository<Role,Integer> {
    
    Optional<Role> findByRoleName(String roleName);
}
