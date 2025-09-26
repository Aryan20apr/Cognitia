package com.intellidesk.cognitia.ingestion.storage.service.uploadStrategy;

import java.util.List;
import java.util.Map;

import com.intellidesk.cognitia.ingestion.storage.models.dtos.CloudinaryUploadResult;

public class CloudinaryMapper {

    public static CloudinaryUploadResult fromMap(Map<String, Object> map) {
        if (map == null) return null;

        return CloudinaryUploadResult.builder()
                .publicId((String) map.get("public_id"))
                .version(map.get("version") != null ? ((Number) map.get("version")).longValue() : null)
                .format((String) map.get("format"))
                .resourceType((String) map.get("resource_type"))
                .bytes(map.get("bytes") != null ? ((Number) map.get("bytes")).longValue() : null)
                .url((String) map.get("url"))
                .secureUrl((String) map.get("secure_url"))
                .signature((String) map.get("signature"))
                .originalFilename((String) map.get("original_filename"))
                .type((String) map.get("type"))
                .createdAt((String) map.get("created_at"))
                .width(map.get("width") != null ? ((Number) map.get("width")).intValue() : null)
                .height(map.get("height") != null ? ((Number) map.get("height")).intValue() : null)
                .tags(map.get("tags") != null ? (List<String>) map.get("tags") : null)
                .build();
    }
}

