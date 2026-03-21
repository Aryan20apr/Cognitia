package com.intellidesk.cognitia.ingestion.service.transformer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentContextExtractor {

    private final ChatClient chatClient;
    private final int contextChunks;

    public DocumentContextExtractor(ChatClient chatClient, int contextChunks) {
        this.chatClient = chatClient;
        this.contextChunks = contextChunks;
    }

    record LlmExtractionResult(String title, String description, String entities) {}

    public DocumentContext extract(
            List<Document> rawDocuments,
            List<Document> chunks,
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {

        String tikaTitle = extractTikaField(rawDocuments, "dc:title", "title");
        String tikaAuthor = extractTikaField(rawDocuments, "dc:creator", "Author");
        String tikaDate = extractTikaField(rawDocuments, "dcterms:created", "date", "Creation-Date");
        int pageCount = extractPageCount(rawDocuments);

        String openingText = chunks.stream()
                .limit(contextChunks)
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        try {
            String prompt = buildExtractionPrompt(rawSource.getName(), tikaTitle, tikaAuthor, tikaDate, pageCount, openingText);

            LlmExtractionResult result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(LlmExtractionResult.class);

            String title = (result.title() != null && !result.title().isBlank())
                    ? result.title().trim()
                    : (tikaTitle != null ? tikaTitle : rawSource.getName());

            return new DocumentContext(
                    title,
                    safe(tikaAuthor),
                    safe(tikaDate),
                    safe(result.description()).trim(),
                    safe(result.entities()).trim(),
                    pageCount);
        } catch (Exception e) {
            log.warn("DocumentContextExtractor LLM call failed, falling back to Tika metadata: {}", e.getMessage());
            return fallback(tikaTitle, tikaAuthor, tikaDate, pageCount, rawSource.getName());
        }
    }

    private String buildExtractionPrompt(String fileName, String tikaTitle, String tikaAuthor,
                                          String tikaDate, int pageCount, String openingText) {
        return """
                Document metadata:
                - File name: %s
                - Title: %s
                - Author: %s
                - Date: %s
                - Pages: %d

                Opening content:
                %s

                Based on the metadata and opening content, extract:
                1. A corrected/refined document title
                2. A 2-3 sentence description of what this document is about
                3. Key entities mentioned (companies, people, products, dates, locations)
                """.formatted(
                safe(fileName), safe(tikaTitle), safe(tikaAuthor),
                safe(tikaDate), pageCount,
                truncate(openingText, 6000));
    }

    private DocumentContext fallback(String tikaTitle, String tikaAuthor, String tikaDate,
                                      int pageCount, String fileName) {
        String title = (tikaTitle != null && !tikaTitle.isBlank()) ? tikaTitle : fileName;
        return new DocumentContext(title, safe(tikaAuthor), safe(tikaDate), "", "", pageCount);
    }

    private String extractTikaField(List<Document> rawDocuments, String... keys) {
        for (Document doc : rawDocuments) {
            Map<String, Object> metadata = doc.getMetadata();
            for (String key : keys) {
                Object val = metadata.get(key);
                if (val != null && !val.toString().isBlank()) {
                    return val.toString();
                }
            }
        }
        return null;
    }

    private int extractPageCount(List<Document> rawDocuments) {
        for (Document doc : rawDocuments) {
            Object pages = doc.getMetadata().get("xmpTPg:NPages");
            if (pages != null) {
                try {
                    return Integer.parseInt(pages.toString());
                } catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "...";
    }
}
