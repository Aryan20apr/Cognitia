package com.intellidesk.cognitia.chat.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.chat.models.dtos.AccessPolicy;
import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;
import com.intellidesk.cognitia.userandauth.models.entities.Department;
import com.intellidesk.cognitia.userandauth.models.entities.Role;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.models.entities.enums.RoleEnum;
import com.intellidesk.cognitia.userandauth.repository.ClassificationLevelRepository;
import com.intellidesk.cognitia.userandauth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessPolicyResolver {

    private final ClassificationLevelRepository classificationLevelRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AccessPolicy resolve(UUID userId) {
        User user = userRepository.findByIdWithAccessData(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Role role = user.getRole();

        boolean isSuperAdmin = role != null
                && RoleEnum.SUPER_ADMIN.name().equals(role.getRoleName());

        int clearanceRank = (role != null && role.getClearanceLevel() != null
                && role.getClearanceLevel().getRank() != null)
                ? role.getClearanceLevel().getRank()
                : 0;

        Optional<ClassificationLevel> maxLevel =
                classificationLevelRepository.findFirstByTenantIdOrderByRankDesc(user.getTenantId());
        boolean hasMaxClearance = maxLevel.isPresent() && clearanceRank >= maxLevel.get().getRank();

        boolean unrestricted = isSuperAdmin || hasMaxClearance;

        Set<String> departmentNames = new HashSet<>();
        departmentNames.add("General");
        if (user.getDepartments() != null) {
            departmentNames.addAll(
                user.getDepartments().stream()
                    .map(Department::getName)
                    .collect(Collectors.toSet())
            );
        }

        return new AccessPolicy(
            user.getTenantId(),
            departmentNames,
            clearanceRank,
            unrestricted
        );
    }
}
