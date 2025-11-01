package com.intellidesk.cognitia.analytics.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.intellidesk.cognitia.analytics.models.entity.ChatUsage;

public interface ChatUsageRepository extends JpaRepository<ChatUsage, UUID>, JpaSpecificationExecutor<ChatUsage> {

}

