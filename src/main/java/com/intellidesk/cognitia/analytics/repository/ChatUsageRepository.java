package com.intellidesk.cognitia.analytics.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.intellidesk.cognitia.analytics.models.entity.ChatUsage;

public interface ChatUsageRepository extends JpaRepository<ChatUsage, UUID>, JpaSpecificationExecutor<ChatUsage> {

     @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ChatUsage c WHERE c.requestId = :requestId")
     boolean existsByRequestId(String requestId);

     Optional<ChatUsage> findByRequestId(String requestId);
}

