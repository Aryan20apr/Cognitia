package com.intellidesk.cognitia.analytics.service.impl;


import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.PlanDTO;
import com.intellidesk.cognitia.analytics.models.entity.Plan;
import com.intellidesk.cognitia.analytics.repository.PlanRepository;
import com.intellidesk.cognitia.analytics.service.PlanCatalogService;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.PlanNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class PlanCatalogServiceImpl implements PlanCatalogService {

    private final PlanRepository planRepo;

    public PlanCatalogServiceImpl(PlanRepository planRepo) {
        this.planRepo = planRepo;
    }

    @Override
    public List<PlanDTO> listPlans() {
        return planRepo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public PlanDTO findById(UUID id) {
        return planRepo.findById(id).map(this::toDto).orElseThrow(() -> new PlanNotFoundException("Plan not found"));
    }

    @Override
    public PlanDTO findByCode(String code) {
        log.info("Finding plan by code: {}",code);

        Optional<Plan> plan = planRepo.findByCode(code);
        if(plan.isPresent()){
            return toDto(plan.get());
        } else{
            throw new PlanNotFoundException("Plan not found with code "+code);
        }
        
    }

    @Override
    public PlanDTO createPlan(PlanDTO dto) {
        Plan p = new Plan();
        p.setCode(dto.getCode());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setIncludedPromptTokens(dto.getIncludedPromptTokens());
        p.setIncludedCompletionTokens(dto.getIncludedCompletionTokens());
        p.setIncludedTotalTokens(dto.getIncludedTotalTokens());
        p.setIncludedDocs(dto.getIncludedDocs());
        p.setIncludedUsers(dto.getIncludedUsers());
        p.setPricePerMonth(dto.getPricePerMonth());
        p.setOveragePricing(dto.getOveragePer1KTokens());
        p.setModelRestrictions(dto.getModelRestriction());
        p.setActive(true);
        Plan saved = planRepo.save(p);
        return toDto(saved);
    }

    @Override
    @Transactional
    public PlanDTO updatePlan(UUID id, PlanDTO dto) {
        Plan p = planRepo.findById(id).orElseThrow(() -> new PlanNotFoundException("Plan not found"));
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setIncludedPromptTokens(dto.getIncludedPromptTokens());
        p.setIncludedCompletionTokens(dto.getIncludedCompletionTokens());
        p.setIncludedTotalTokens(dto.getIncludedTotalTokens());
        p.setIncludedDocs(dto.getIncludedDocs());
        p.setIncludedUsers(dto.getIncludedUsers());
        p.setPricePerMonth(dto.getPricePerMonth());
        p.setOveragePricing(dto.getOveragePer1KTokens());
        p.setModelRestrictions(dto.getModelRestriction());
        Plan saved = planRepo.save(p);
        return toDto(saved);
    }

    @Override
    public void deactivatePlan(UUID id) {
        Plan p = planRepo.findById(id).orElseThrow(() -> new PlanNotFoundException("Plan not found"));
        p.setActive(false);
        planRepo.save(p);
    }

    private PlanDTO toDto(Plan p) {
        log.info("Converting plan to dto: {}", p);
        PlanDTO d = new PlanDTO();
        d.setId(p.getId());
        d.setCode(p.getCode());
        d.setName(p.getName());
        d.setDescription(p.getDescription());
        d.setIncludedPromptTokens(p.getIncludedPromptTokens());
        d.setIncludedCompletionTokens(p.getIncludedCompletionTokens());
        d.setIncludedTotalTokens(p.getIncludedTotalTokens());
        d.setIncludedDocs(p.getIncludedDocs());
        d.setIncludedUsers(p.getIncludedUsers());
        d.setPricePerMonth(p.getPricePerMonth());
        d.setOveragePer1KTokens(p.getOveragePricing());
        d.setModelRestriction(p.getModelRestrictions());
        return d;
    }
}

