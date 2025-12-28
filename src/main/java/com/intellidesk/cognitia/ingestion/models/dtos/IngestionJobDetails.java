package com.intellidesk.cognitia.ingestion.models.dtos;

import java.util.Date;
import java.util.UUID;

import com.intellidesk.cognitia.ingestion.models.enums.IngestionStatus;
import com.intellidesk.cognitia.ingestion.models.enums.Status;

public record IngestionJobDetails(
    UUID outboxId,
    IngestionStatus ingestionStatus,
    Integer retries,
    UUID resourceId,
    String resourceName,
    Status resourceStatus,
    Date createdAt
) {}
