package com.intellidesk.cognitia.analytics.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.intellidesk.cognitia.analytics.models.entity.Plan;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
     Optional<Plan> findByCode(String code);
}
