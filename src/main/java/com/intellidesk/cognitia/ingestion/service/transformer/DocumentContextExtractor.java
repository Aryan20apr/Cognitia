package com.intellidesk.cognitia.ingestion.service.transformer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import com.intellidesk.cognitia.userandauth.models.entities.ClassificationLevel;
import com.intellidesk.cognitia.userandauth.models.entities.Department;
import com.intellidesk.cognitia.userandauth.repository.ClassificationLevelRepository;
import com.intellidesk.cognitia.userandauth.repository.DepartmentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentContextExtractor {

    private final ChatClient chatClient;
    private final int contextChunks;
    private final DepartmentRepository departmentRepository;
    private final ClassificationLevelRepository classificationLevelRepository;

    public DocumentContextExtractor(ChatClient chatClient, int contextChunks,
                                     DepartmentRepository departmentRepository,
                                     ClassificationLevelRepository classificationLevelRepository) {
        this.chatClient = chatClient;
        this.contextChunks = contextChunks;
        this.departmentRepository = departmentRepository;
        this.classificationLevelRepository = classificationLevelRepository;
    }

    record LlmExtractionResult(String title, String description, String entities,
                                String department, String classification) {}

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

        boolean needsDepartment = rawSource.getDepartment() == null;
        boolean needsClassification = rawSource.getClassificationLevel() == null;

        UUID tenantId = rawSource.getTenantId();
        List<String> tenantDepartments = needsDepartment
                ? departmentRepository.findByTenantId(tenantId).stream().map(Department::getName).toList()
                : List.of();
        List<String> tenantClassifications = needsClassification
                ? classificationLevelRepository.findByTenantIdOrderByRankAsc(tenantId).stream().map(ClassificationLevel::getName).toList()
                : List.of();

        try {
            String prompt = buildExtractionPrompt(rawSource.getName(), tikaTitle, tikaAuthor, tikaDate, pageCount,
                    openingText, needsDepartment, tenantDepartments, needsClassification, tenantClassifications);

            LlmExtractionResult result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(LlmExtractionResult.class);

            String title = (result.title() != null && !result.title().isBlank())
                    ? result.title().trim()
                    : (tikaTitle != null ? tikaTitle : rawSource.getName());

            String departmentName = null;
            String classificationName = null;

            if (needsDepartment && result.department() != null) {
                departmentName = resolveAndSetDepartment(result.department().trim(), tenantId, rawSource);
            }
            if (needsClassification && result.classification() != null) {
                classificationName = resolveAndSetClassification(result.classification().trim(), tenantId, rawSource);
            }

            return new DocumentContext(
                    title,
                    safe(tikaAuthor),
                    safe(tikaDate),
                    safe(result.description()).trim(),
                    safe(result.entities()).trim(),
                    pageCount,
                    departmentName,
                    classificationName);
        } catch (Exception e) {
            log.warn("DocumentContextExtractor LLM call failed, falling back to Tika metadata: {}", e.getMessage());
            return fallback(tikaTitle, tikaAuthor, tikaDate, pageCount, rawSource.getName());
        }
    }

    private String resolveAndSetDepartment(String llmDepartment, UUID tenantId,
                                            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {
        Optional<Department> match = departmentRepository.findByTenantId(tenantId).stream()
                .filter(d -> d.getName().equalsIgnoreCase(llmDepartment))
                .findFirst();

        Department resolved = match.orElseGet(() ->
                departmentRepository.findByTenantIdAndName(tenantId, "General")
                        .orElse(null));

        if (resolved != null) {
            rawSource.setDepartment(resolved);
            return resolved.getName();
        }
        return "General";
    }

    private String resolveAndSetClassification(String llmClassification, UUID tenantId,
                                                com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {
        List<ClassificationLevel> levels = classificationLevelRepository.findByTenantIdOrderByRankAsc(tenantId);

        Optional<ClassificationLevel> match = levels.stream()
                .filter(cl -> cl.getName().equalsIgnoreCase(llmClassification))
                .findFirst();

        // Default to lowest rank if LLM response doesn't match any configured level
        ClassificationLevel resolved = match.orElseGet(() ->
                levels.isEmpty() ? null : levels.get(0));

        if (resolved != null) {
            rawSource.setClassificationLevel(resolved);
            return resolved.getName();
        }
        return null;
    }

    private String buildExtractionPrompt(String fileName, String tikaTitle, String tikaAuthor,
                                          String tikaDate, int pageCount, String openingText,
                                          boolean needsDepartment, List<String> departments,
                                          boolean needsClassification, List<String> classifications) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
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
                truncate(openingText, 6000)));

        if (needsDepartment && !departments.isEmpty()) {
            sb.append("4. Classify this document into one of these departments: [")
              .append(String.join(", ", departments))
              .append("]\n");
        }
        if (needsClassification && !classifications.isEmpty()) {
            sb.append(needsDepartment ? "5" : "4")
              .append(". Assign a sensitivity level from: [")
              .append(String.join(", ", classifications))
              .append("]. Pick the most restrictive level if uncertain.\n");
        }

        return sb.toString();
    }

    private DocumentContext fallback(String tikaTitle, String tikaAuthor, String tikaDate,
                                      int pageCount, String fileName) {
        String title = (tikaTitle != null && !tikaTitle.isBlank()) ? tikaTitle : fileName;
        return new DocumentContext(title, safe(tikaAuthor), safe(tikaDate), "", "", pageCount, null, null);
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
