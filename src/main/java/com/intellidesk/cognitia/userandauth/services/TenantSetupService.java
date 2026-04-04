package com.intellidesk.cognitia.userandauth.services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;
import com.intellidesk.cognitia.userandauth.models.entities.Department;
import com.intellidesk.cognitia.userandauth.repository.ClassificationLevelRepository;
import com.intellidesk.cognitia.userandauth.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSetupService {

    private final DepartmentRepository departmentRepository;
    private final ClassificationLevelRepository classificationLevelRepository;

    private static final List<ClassificationSeed> DEFAULT_CLASSIFICATIONS = List.of(
        new ClassificationSeed("Public", 0),
        new ClassificationSeed("Internal", 10),
        new ClassificationSeed("Confidential", 20),
        new ClassificationSeed("Restricted", 30)
    );

    @Transactional
    public void seedDefaults(UUID tenantId) {
        seedDefaultDepartment(tenantId);
        seedDefaultClassifications(tenantId);
        log.info("[TenantSetupService] Seeded default departments and classifications for tenant {}", tenantId);
    }

    private void seedDefaultDepartment(UUID tenantId) {
        if (departmentRepository.findByTenantIdAndName(tenantId, "General").isEmpty()) {
            Department general = Department.builder()
                .name("General")
                .build();
            general.setTenantId(tenantId);
            departmentRepository.save(general);
        }
    }

    private void seedDefaultClassifications(UUID tenantId) {
        for (ClassificationSeed seed : DEFAULT_CLASSIFICATIONS) {
            if (classificationLevelRepository.findByTenantIdAndName(tenantId, seed.name()).isEmpty()) {
                ClassificationLevel level = ClassificationLevel.builder()
                    .name(seed.name())
                    .rank(seed.rank())
                    .build();
                level.setTenantId(tenantId);
                classificationLevelRepository.save(level);
            }
        }
    }

    public Optional<UUID> getHighestClearanceLevelId(UUID tenantId) {
        return classificationLevelRepository.findFirstByTenantIdOrderByRankDesc(tenantId)
            .map(ClassificationLevel::getId);
    }

    private record ClassificationSeed(String name, int rank) {}
}
