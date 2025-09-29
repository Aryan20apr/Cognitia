package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import com.intellidesk.cognitia.ingestion.models.entities.RawSouce;

public interface PreprocessingStrategy {
    
     public List<Document> preprocess(Resource resource, RawSouce rawSource);
}
