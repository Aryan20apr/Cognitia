package com.intellidesk.cognitia.ingestion.models.dtos;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ResourceDetails {
    private String assetId;

    private String name;

    private String description;

    private String url;

    private String secureUrl;

    private String signature;

    private String status;

    private String format;

    private Double size;

    private Instant createdAt;

    private Instant updatedAt;
}
