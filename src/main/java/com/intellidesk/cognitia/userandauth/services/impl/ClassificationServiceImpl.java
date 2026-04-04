package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.userandauth.models.dtos.ClassificationLevelDTO;
import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.repository.ClassificationLevelRepository;
import com.intellidesk.cognitia.userandauth.services.ClassificationService;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassificationServiceImpl implements ClassificationService {

    private final ClassificationLevelRepository classificationLevelRepository;

    @Override
    @Transactional
    public ClassificationLevelDTO create(String name, Integer rank) {
        UUID tenantId = TenantContext.getTenantId();
        if (classificationLevelRepository.existsByTenantIdAndName(tenantId, name)) {
            throw new ApiException("Classification level with this name already exists");
        }
        if (classificationLevelRepository.existsByTenantIdAndRank(tenantId, rank)) {
            throw new ApiException("Classification level with this rank already exists");
        }
        ClassificationLevel level = ClassificationLevel.builder()
            .name(name)
            .rank(rank)
            .build();
        level.setTenantId(tenantId);
        ClassificationLevel saved = classificationLevelRepository.save(level);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassificationLevelDTO> getAll() {
        UUID tenantId = TenantContext.getTenantId();
        return classificationLevelRepository.findByTenantIdOrderByRankAsc(tenantId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    @Transactional
    public ClassificationLevelDTO update(UUID id, String name, Integer rank) {
        ClassificationLevel level = classificationLevelRepository.findById(id)
            .orElseThrow(() -> new ApiException("Classification level not found"));
        UUID tenantId = TenantContext.getTenantId();
        if (!level.getTenantId().equals(tenantId)) {
            throw new ApiException("Classification level not found");
        }
        if (name != null && !name.equals(level.getName())
                && classificationLevelRepository.existsByTenantIdAndName(tenantId, name)) {
            throw new ApiException("Classification level with this name already exists");
        }
        if (rank != null && !rank.equals(level.getRank())
                && classificationLevelRepository.existsByTenantIdAndRank(tenantId, rank)) {
            throw new ApiException("Classification level with this rank already exists");
        }
        if (name != null) level.setName(name);
        if (rank != null) level.setRank(rank);
        return toDTO(classificationLevelRepository.save(level));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ClassificationLevel level = classificationLevelRepository.findById(id)
            .orElseThrow(() -> new ApiException("Classification level not found"));
        if (!level.getTenantId().equals(TenantContext.getTenantId())) {
            throw new ApiException("Classification level not found");
        }
        classificationLevelRepository.delete(level);
    }

    private ClassificationLevelDTO toDTO(ClassificationLevel level) {
        return new ClassificationLevelDTO(level.getId(), level.getName(), level.getRank());
    }
}
