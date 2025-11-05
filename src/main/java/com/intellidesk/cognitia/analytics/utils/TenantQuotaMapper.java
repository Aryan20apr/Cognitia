package com.intellidesk.cognitia.analytics.utils;


import com.intellidesk.cognitia.analytics.models.dto.TenantQuotaDTO;
import com.intellidesk.cognitia.analytics.models.entity.TenantQuota;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantQuotaMapper {

    @Mapping(source = "planId.id", target = "planId")
    @Mapping(source = "planId.name", target = "planName")
    TenantQuotaDTO toDto(TenantQuota entity);

    @InheritInverseConfiguration
    TenantQuota toEntity(TenantQuotaDTO dto);
}

