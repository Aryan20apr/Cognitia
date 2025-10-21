package com.intellidesk.cognitia.userandauth.models.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDTO {

    private String id;
    private String name;
    private String about;
    private String domain;
    private String contactEmail;
    private String adminEmail;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String adminPassword;  // writable in request, hidden in response

    private String adminName;
    private String phoneNumber;
}