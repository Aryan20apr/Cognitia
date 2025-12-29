package com.intellidesk.cognitia.ingestion.service;

import org.springframework.data.domain.Page;
import com.intellidesk.cognitia.ingestion.models.dtos.IngestionJobDetails;

public interface IngestionJobService {
    
    public Page<IngestionJobDetails> getIngestionJobDetails(int page, int size);
}
