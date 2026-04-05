package com.intellidesk.cognitia.chat.models.dtos;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record AccessPolicy(
    UUID tenantId,
    Set<String> departmentNames,
    int clearanceRank,
    boolean unrestricted
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String serialize() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize AccessPolicy", e);
        }
    }

    public static AccessPolicy deserialize(String json) {
        try {
            return MAPPER.readValue(json, AccessPolicy.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize AccessPolicy", e);
        }
    }
}
