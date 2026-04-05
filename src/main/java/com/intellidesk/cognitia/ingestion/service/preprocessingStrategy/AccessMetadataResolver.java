package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.ingestion.models.entities.Resource;
import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;
import com.intellidesk.cognitia.userandauth.models.entities.Department;
import com.intellidesk.cognitia.userandauth.repository.ClassificationLevelRepository;
import com.intellidesk.cognitia.userandauth.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ensures every Resource has a department and classification resolved before
 * chunk metadata is stamped. Falls back to "General" / lowest-rank defaults.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessMetadataResolver {

    private final DepartmentRepository departmentRepository;
    private final ClassificationLevelRepository classificationLevelRepository;

    public void ensureDefaults(Resource resource) {
        UUID tenantId = resource.getTenantId();

        if (resource.getDepartment() == null) {
            departmentRepository.findByTenantIdAndName(tenantId, "General")
                .ifPresent(resource::setDepartment);
            if (resource.getDepartment() == null) {
                log.warn("No 'General' department found for tenant {}, department metadata will be empty", tenantId);
            }
        }

        if (resource.getClassificationLevel() == null) {
            classificationLevelRepository.findFirstByTenantIdOrderByRankAsc(tenantId)
                .ifPresent(resource::setClassificationLevel);
            if (resource.getClassificationLevel() == null) {
                log.warn("No classification levels found for tenant {}, classification metadata will be empty", tenantId);
            }
        }
    }

    public String getDepartmentName(Resource resource) {
        Department dept = resource.getDepartment();
        return dept != null ? dept.getName() : "General";
    }

    public String getClassificationRank(Resource resource) {
        ClassificationLevel cl = resource.getClassificationLevel();
        return cl != null ? String.valueOf(cl.getRank()) : "0";
    }
}
