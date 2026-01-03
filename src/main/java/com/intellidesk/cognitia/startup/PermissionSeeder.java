package com.intellidesk.cognitia.startup;



import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.userandauth.models.entities.Permission;
import com.intellidesk.cognitia.userandauth.models.entities.enums.Permissions;
import com.intellidesk.cognitia.userandauth.repository.PermissionsRepository;

import lombok.extern.slf4j.Slf4j;

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
        List<String> defaultPermissions = Arrays.stream(Permissions.values())
            .map(Enum::name)
            .toList();

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

