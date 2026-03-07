package com.intellidesk.cognitia.chat.service.tools;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Agentic RAG tool -- lets the LLM search the tenant's ingested knowledge base
 * on-demand with a focused query, instead of always pre-fetching docs.
 */
@Component
@Slf4j
public class KnowledgeSearchTool implements TimelineAwareTool {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.50;

    public KnowledgeSearchTool(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Search the user's ingested knowledge base (uploaded documents, files, and resources) "
            + "for information relevant to a specific query. Use this when the user asks about their own documents, "
            + "internal policies, reports, or any content they have previously uploaded. "
            + "You can call this multiple times with different queries to gather comprehensive information. "
            + "You can also filter by document format (e.g. 'pdf', 'docx', 'txt').",
            returnDirect = false)
    public List<KnowledgeResult> searchKnowledge(
            @ToolParam(description = "A focused, specific search query describing what information to find "
                    + "in the knowledge base. Be precise -- e.g. 'Q3 revenue figures' rather than 'tell me about Q3'.")
            String query,

            @ToolParam(description = "Maximum number of document chunks to return (1-10). "
                    + "Use fewer for focused questions, more for broad topics. Defaults to 5.")
            Integer topK,

            @ToolParam(description = "Optional: filter by document format. "
                    + "Examples: 'pdf', 'docx', 'txt', 'csv'. Leave null to search all formats.",
                    required = false)
            String sourceFormat) {

        int resolvedTopK = (topK != null && topK >= 1 && topK <= 10) ? topK : DEFAULT_TOP_K;
        String filterExpression = buildFilterExpression(sourceFormat);

        log.info("KnowledgeSearch - query='{}', topK={}, filter='{}'", query, resolvedTopK, filterExpression);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(resolvedTopK)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);

        if (filterExpression != null) {
            builder.filterExpression(filterExpression);
        }

        try {
            List<Document> documents = vectorStore.similaritySearch(builder.build());
            log.info("KnowledgeSearch - found {} documents for query='{}'", documents.size(), query);
            return documents.stream().map(this::toKnowledgeResult).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("KnowledgeSearch failed for query='{}': {}", query, e.getMessage(), e);
            return List.of();
        }
    }

    private String buildFilterExpression(String sourceFormat) {
        if (sourceFormat != null && !sourceFormat.isBlank()) {
            return "sourceFormat == '" + sourceFormat.strip() + "'";
        }
        return null;
    }

    private KnowledgeResult toKnowledgeResult(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        return new KnowledgeResult(
                doc.getText(),
                (String) metadata.get("sourceId"),
                (String) metadata.get("sourceUrl"),
                (String) metadata.get("sourceFormat")
        );
    }

    @Override
    public String timelineDescription() {
        return "Searching knowledge base";
    }

    @Override
    public String summarizeResult(String rawJsonResult) {
        if (rawJsonResult == null || rawJsonResult.isBlank()) return "No documents found";
        try {
            List<?> results = objectMapper.readValue(rawJsonResult, List.class);
            int count = results.size();
            if (count == 0) return "No relevant documents found";
            return "Found " + count + " relevant document" + (count != 1 ? "s" : "") + " from knowledge base";
        } catch (Exception e) {
            return "Knowledge search completed";
        }
    }

    public record KnowledgeResult(
            String content,
            String sourceId,
            String sourceUrl,
            String sourceFormat
    ) {}
}
