package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.ingestion.service.transformer.ContextualChunkEnricher;
import com.intellidesk.cognitia.ingestion.service.transformer.DocumentContext;
import com.intellidesk.cognitia.ingestion.service.transformer.DocumentContextExtractor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentPreprocessingStrategy implements PreprocessingStrategy {

    private final TextSplitter textSplitter;
    private final DocumentWriter documentWriter;
    private final DocumentContextExtractor documentContextExtractor;
    private final ContextualChunkEnricher contextualChunkEnricher;
    private final AccessMetadataResolver accessMetadataResolver;
    private final boolean enrichmentEnabled;

    DocumentPreprocessingStrategy(TextSplitter textSplitter,
                                  DocumentWriter documentWriter,
                                  DocumentContextExtractor documentContextExtractor,
                                  ContextualChunkEnricher contextualChunkEnricher,
                                  AccessMetadataResolver accessMetadataResolver,
                                  @Value("${ingestion.contextual-enrichment.enabled:true}") boolean enrichmentEnabled) {
        this.textSplitter = textSplitter;
        this.documentWriter = documentWriter;
        this.documentContextExtractor = documentContextExtractor;
        this.contextualChunkEnricher = contextualChunkEnricher;
        this.accessMetadataResolver = accessMetadataResolver;
        this.enrichmentEnabled = enrichmentEnabled;
    }

    public List<Document> preprocess(Resource resource, com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource ){
        
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        List<Document> rawDocuments = tikaDocumentReader.read();

        List<Document> documents = textSplitter.apply(rawDocuments);

        if (enrichmentEnabled) {
            try {
                DocumentContext docContext = documentContextExtractor.extract(rawDocuments, documents, rawSource);
                String serializedContext = docContext.serialize();
                documents.forEach(doc -> doc.getMetadata().put(ContextualChunkEnricher.METADATA_KEY, serializedContext));
                documents = contextualChunkEnricher.apply(documents);
                documents.forEach(doc -> doc.getMetadata().remove(ContextualChunkEnricher.METADATA_KEY));
                log.info("Contextual enrichment completed for resource: {}", rawSource.getName());
            } catch (Exception e) {
                log.error("Contextual enrichment failed for resource {}, proceeding with unenriched chunks: {}",
                        rawSource.getName(), e.getMessage());
            }
        }

        accessMetadataResolver.ensureDefaults(rawSource);

        String ingestionTimestamp = Instant.now().toString();
        String contentType = resolveContentType(rawSource.getFormat());
        String departmentName = accessMetadataResolver.getDepartmentName(rawSource);
        String classificationRank = accessMetadataResolver.getClassificationRank(rawSource);
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
            metaData.put("department", departmentName);
            metaData.put("classificationRank", classificationRank);

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
