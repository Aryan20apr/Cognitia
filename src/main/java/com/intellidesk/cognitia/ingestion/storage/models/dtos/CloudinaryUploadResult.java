package com.intellidesk.cognitia.ingestion.storage.models.dtos;

import lombok.Builder;

@Builder
public record CloudinaryUploadResult(
    String publicId,
    Long version,
    String format,
    String resourceType,
    Long bytes,
    String url,
    String secureUrl,
    String signature,
    String originalFilename,
    String type,
    String assetId,
    String createdAt,
    Integer width,
    Integer height,
    java.util.List<String> tags
) {}

