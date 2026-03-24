package com.intellidesk.cognitia.userandauth.models.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Tenant/company update payload")
public record TenantUpdateDTO(
        @Size(min = 1, max = 200)
        String name,

        String about,

        String domain,

        @Email
        String contactEmail
) {}
