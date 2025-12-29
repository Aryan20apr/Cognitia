package com.intellidesk.cognitia.userandauth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.intellidesk.cognitia.userandauth.models.entities.User;

public interface UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "role")
    List<User> findAll();

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithRoleAndPermissions(@Param("email") String email);

    boolean existsByEmailOrPhoneNumber(String email, String phoneNumber);
}
