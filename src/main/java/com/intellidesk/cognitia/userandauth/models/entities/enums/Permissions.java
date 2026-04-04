package com.intellidesk.cognitia.userandauth.models.entities.enums;

import java.util.Set;

public enum Permissions {
    USER_CREATE,
    USER_READ,
    USER_UPDATE,
    USER_DELETE,
    USER_ASSIGN_ROLE,
    ROLE_CREATE,
    ROLE_READ,
    ROLE_UPDATE,
    ROLE_DELETE,
    PLAN_CREATE,
    PLAN_READ,
    PLAN_UPDATE,
    PLAN_DELETE,
    PERMISSION_READ,
    TENANT_READ,
    TENANT_CREATE,
    TENANT_UPDATE,
    TENANT_DELETE,
    RESOURCE_CREATE,
    RESOURCE_UPDATE,
    RESOURCE_GET,
    RESOURCE_DELETE,
    CHAT_ACCESS,
    INGESTION_READ,
    ORDER_CREATE,
    ORDER_READ,
    PAYMENT_VERIFY,
    ANALYTICS_READ,
    TENANT_LIST,
    QUOTA_READ,
    QUOTA_ADMIN,
    DEPARTMENT_MANAGE,
    CLASSIFICATION_MANAGE;

    private static final Set<Permissions> PLATFORM_LEVEL = Set.of(
            PLAN_CREATE, PLAN_UPDATE, PLAN_DELETE, PLAN_READ,
            TENANT_LIST, QUOTA_ADMIN
    );

    public static boolean isPlatformLevel(Permissions permission) {
        return PLATFORM_LEVEL.contains(permission);
    }
}
