package com.intellidesk.cognitia.analytics.utils;

import org.mapstruct.*;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.models.entity.ChatUsage;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
@Component
public interface ChatUsageMapper {

    // ---------- Entity → DTO ----------
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "tenantId", target = "tenantId")
    @Mapping(source = "thread.id", target = "threadId")
    ChatUsageDetailsDTO toDTO(ChatUsage entity);

    // ---------- DTO → Entity ----------
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "thread", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    ChatUsage toEntity(ChatUsageDetailsDTO dto);
}
