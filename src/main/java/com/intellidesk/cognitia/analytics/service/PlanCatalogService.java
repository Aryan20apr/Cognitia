package com.intellidesk.cognitia.analytics.service;


import java.util.List;
import java.util.UUID;

import com.intellidesk.cognitia.analytics.models.dto.PlanDTO;

public interface PlanCatalogService {
    List<PlanDTO> listPlans();
    PlanDTO findById(UUID id);
    PlanDTO findByCode(String code);
    PlanDTO createPlan(PlanDTO dto);
    PlanDTO updatePlan(UUID id, PlanDTO dto);
    void deactivatePlan(UUID id);
}

