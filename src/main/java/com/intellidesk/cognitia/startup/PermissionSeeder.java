package com.intellidesk.cognitia.startup;



import com.intellidesk.cognitia.userandauth.models.entities.Permission;
import com.intellidesk.cognitia.userandauth.repository.PermissionsRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
@Slf4j
public class PermissionSeeder implements CommandLineRunner {

    private final PermissionsRepository permissionRepository;
    public PermissionSeeder(PermissionsRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) {
        seedPermissions();
    }

    private void seedPermissions() {
        List<String> defaultPermissions = List.of(
            "USER_CREATE",
            "USER_READ",
            "USER_UPDATE",
            "USER_DELETE",
            "ROLE_CREATE",
            "ROLE_READ",
            "ROLE_UPDATE",
            "ROLE_DELETE",
            "PERMISSION_READ",
            "TENANT_READ",
            "TENANT_CREATE",
            "TENANT_UPDATE",
            "TENANT_DELETE",
            "TENANT_GET",
            "RESOURCE_CREATE",
            "RESOURCE_UPDATE",
            "RESOURCE_GET",
            "RESOURCE_DELETE"
        );

        for (String permissionName : defaultPermissions) {
            permissionRepository.findByName(permissionName)
                .orElseGet(() -> {
                    Permission p = Permission.builder().name(permissionName).build();
                    return permissionRepository.save(p);
                });
        }

        log.info("✅ Permissions seeded successfully.");
    }
}

