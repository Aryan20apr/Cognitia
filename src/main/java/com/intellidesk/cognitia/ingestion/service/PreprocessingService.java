package com.intellidesk.cognitia.ingestion.service;

import com.intellidesk.cognitia.ingestion.models.entities.IngestionJob;

public interface PreprocessingService {
    
    public void preprocessFile(IngestionJob ingestoionOutbox);
}
