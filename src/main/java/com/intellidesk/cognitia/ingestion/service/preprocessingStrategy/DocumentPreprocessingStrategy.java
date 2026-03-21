package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

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

    public List<Document> preprocess(Resource resource, com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource ){
        
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        List<Document> documents = tikaDocumentReader.read();

        documents = textSplitter.apply(documents);

        String ingestionTimestamp = Instant.now().toString();
        String contentType = resolveContentType(rawSource.getFormat());
        AtomicInteger chunkIndex = new AtomicInteger(1);

        documents.stream().forEach(doc -> {
            Map<String, Object> metaData = doc.getMetadata();
            metaData.put("tenantId", rawSource.getTenantId().toString());
            metaData.put("sourceId", rawSource.getResId().toString());
            metaData.put("sourceName", rawSource.getName());
            metaData.put("sourceUrl", rawSource.getUrl());
            metaData.put("sourceFormat", rawSource.getFormat());
            metaData.put("fileName", rawSource.getName());
            metaData.put("ingestionTimestamp", ingestionTimestamp);
            metaData.put("contentType", contentType);

            Object tikaPage = metaData.get("page_number");
            if (tikaPage == null) {
                tikaPage = metaData.get("xmpTPg:NPages");
            }
            if (tikaPage != null) {
                metaData.put("pageNumber", tikaPage.toString());
            } else {
                metaData.put("pageNumber", String.valueOf(chunkIndex.getAndIncrement()));
            }
        });

        documentWriter.write(documents);

        return documents;
    }

    private String resolveContentType(String format) {
        if (format == null) return "application/octet-stream";
        String ext = format.trim().toLowerCase();
        if (ext.startsWith(".")) ext = ext.substring(1);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc" -> "application/msword";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "rtf" -> "application/rtf";
            case "html" -> "text/html";
            case "epub" -> "application/epub+zip";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            case "odp" -> "application/vnd.oasis.opendocument.presentation";
            default -> "application/octet-stream";
        };
    }
}
