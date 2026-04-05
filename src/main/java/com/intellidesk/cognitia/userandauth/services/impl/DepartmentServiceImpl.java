package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.userandauth.models.dtos.DepartmentDTO;
import com.intellidesk.cognitia.userandauth.models.entities.Department;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.repository.DepartmentRepository;
import com.intellidesk.cognitia.userandauth.services.DepartmentService;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public DepartmentDTO create(DepartmentDTO dto) {
        UUID tenantId = TenantContext.getTenantId();
        if (departmentRepository.existsByTenantIdAndName(tenantId, dto.name())) {
            throw new ApiException("Department with this name already exists");
        }
        Department dept = Department.builder()
            .name(dto.name())
            .description(dto.description())
            .build();
        dept.setTenantId(tenantId);
        Department saved = departmentRepository.save(dept);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAll() {
        UUID tenantId = TenantContext.getTenantId();
        return departmentRepository.findByTenantId(tenantId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Override
    @Transactional
    public DepartmentDTO update(UUID id, DepartmentDTO dto) {
        Department dept = departmentRepository.findById(id)
            .orElseThrow(() -> new ApiException("Department not found"));
        UUID tenantId = TenantContext.getTenantId();
        if (!dept.getTenantId().equals(tenantId)) {
            throw new ApiException("Department not found");
        }
        if (!dept.getName().equals(dto.name()) && departmentRepository.existsByTenantIdAndName(tenantId, dto.name())) {
            throw new ApiException("Department with this name already exists");
        }
        dept.setName(dto.name());
        dept.setDescription(dto.description());
        return toDTO(departmentRepository.save(dept));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Department dept = departmentRepository.findById(id)
            .orElseThrow(() -> new ApiException("Department not found"));
        if (!dept.getTenantId().equals(TenantContext.getTenantId())) {
            throw new ApiException("Department not found");
        }
        if ("General".equals(dept.getName())) {
            throw new ApiException("Cannot delete the default General department");
        }
        departmentRepository.delete(dept);
    }

    private DepartmentDTO toDTO(Department dept) {
        return new DepartmentDTO(dept.getId(), dept.getName(), dept.getDescription());
    }
}
