package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.ingestion.models.entities.RawSouce;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentPreprocessingStrategy implements PreprocessingStrategy {

    private TextSplitter textSplitter;
    private DocumentWriter documentWriter;

    DocumentPreprocessingStrategy(TextSplitter textSplitter, DocumentWriter documentWriter){
        this.textSplitter = textSplitter;
        this.documentWriter = documentWriter;
    }

    public List<Document> preprocess(Resource resource, RawSouce rawSource ){
        
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        List<Document> documents = tikaDocumentReader.read();

        documents = textSplitter.apply(documents);

        documents.stream().forEach(doc -> {
            Map<String, Object> metaData = doc.getMetadata();
            metaData.put("sourceId", rawSource.getResId().toString());
            metaData.put("sourceUrl", rawSource.getUrl());
            metaData.put("sourceFormat", rawSource.getFormat());
        });

        documentWriter.write(documents);

        return documents;
    }
    
    
}
