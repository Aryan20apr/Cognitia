package com.intellidesk.cognitia.ingestion.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.intellidesk.cognitia.ingestion.models.dtos.ResourceDetails;
import com.intellidesk.cognitia.ingestion.models.entities.RawSouce;
import com.intellidesk.cognitia.ingestion.models.enums.Status;

@Mapper(componentModel = "spring")
public interface ResourceMapper {

    @Mapping(target = "status", source = "status")
    ResourceDetails toDto(RawSouce entity);

    // ---------- Custom mappings ----------

    default String map(Status status) {
        return status == null ? null : status.name();
    }
}
