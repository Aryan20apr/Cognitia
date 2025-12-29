package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;


public interface PreprocessingStrategy {
    
     public List<Document> preprocess(Resource resource, com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource);
}
