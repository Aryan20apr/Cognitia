package com.intellidesk.cognitia.ingestion.service;

import com.intellidesk.cognitia.ingestion.models.entities.IngestionOutbox;

public interface PreprocessingService {
    
    public void preprocessFile(IngestionOutbox ingestoionOutbox);
}
