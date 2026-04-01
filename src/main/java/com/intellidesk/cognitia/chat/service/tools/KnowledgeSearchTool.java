package com.intellidesk.cognitia.chat.service.tools;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.chat.models.dtos.SourceReference;

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
    private final ChatClient lightClient;
    private final ChatMemory chatMemory;
    private final boolean contextualSearchEnabled;
    private final int historyWindow;

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.50;

    public KnowledgeSearchTool(VectorStore vectorStore,
                               ObjectMapper objectMapper,
                               @Qualifier("lightClient") ChatClient lightClient,
                               ChatMemory chatMemory,
                               @Value("${cognitia.chat.contextual-search.enabled:true}") boolean contextualSearchEnabled,
                               @Value("${cognitia.chat.contextual-search.history-window:5}") int historyWindow) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.lightClient = lightClient;
        this.chatMemory = chatMemory;
        this.contextualSearchEnabled = contextualSearchEnabled;
        this.historyWindow = historyWindow;
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
            String sourceFormat,

            ToolContext toolContext) {

        UUID tenantId = resolveTenantId(toolContext);
        if (tenantId == null) {
            log.error("KnowledgeSearch aborted — no tenant context available");
            return List.of();
        }

        int resolvedTopK = (topK != null && topK >= 1 && topK <= 10) ? topK : DEFAULT_TOP_K;
        String filterExpression = buildFilterExpression(tenantId, sourceFormat);

        String searchQuery = query;
        if (contextualSearchEnabled) {
            searchQuery = rewriteQueryWithContext(query, toolContext);
        }

        log.info("KnowledgeSearch - originalQuery='{}', searchQuery='{}', topK={}, tenantId={}, filter='{}'",
                query, searchQuery, resolvedTopK, tenantId, filterExpression);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(searchQuery)
                .topK(resolvedTopK)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                .filterExpression(filterExpression);

        try {
            List<Document> documents = vectorStore.similaritySearch(builder.build());
            log.info("KnowledgeSearch - found {} documents for query='{}'", documents.size(), query);
            return documents.stream().map(this::toKnowledgeResult).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("KnowledgeSearch failed for query='{}': {}", query, e.getMessage(), e);
            return List.of();
        }
    }

    private UUID resolveTenantId(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext().containsKey("tenantId")) {
            return UUID.fromString((String) toolContext.getContext().get("tenantId"));
        }
        return null;
    }

    private String buildFilterExpression(UUID tenantId, String sourceFormat) {
        String tenantFilter = "tenantId == '" + tenantId.toString() + "'";
        if (sourceFormat != null && !sourceFormat.isBlank()) {
            return tenantFilter + " && sourceFormat == '" + sourceFormat.strip() + "'";
        }
        return tenantFilter;
    }

    private String rewriteQueryWithContext(String originalQuery, ToolContext toolContext) {
        try {
            String conversationId = resolveConversationId(toolContext);
            if (conversationId == null) return originalQuery;

            List<Message> messages = chatMemory.get(conversationId);
            if (messages == null || messages.isEmpty()) return originalQuery;

            int limit = Math.min(messages.size(), historyWindow * 2);
            List<Message> recentMessages = messages.subList(messages.size() - limit, messages.size());

            String recentHistory = recentMessages.stream()
                    .map(m -> m.getMessageType().name() + ": " + truncateText(m.getText(), 200))
                    .collect(Collectors.joining("\n"));

            String rewritten = lightClient.prompt()
                    .user("""
                            Given this conversation history:
                            %s

                            The user is now searching for: %s

                            Rewrite this as an optimized search query that incorporates relevant context from the conversation. \
                            Resolve pronouns and references into concrete terms. Return only the rewritten query and nothing else.
                            """.formatted(recentHistory, originalQuery))
                    .call()
                    .content();

            if (rewritten != null && !rewritten.isBlank()) {
                log.debug("Query rewritten: '{}' -> '{}'", originalQuery, rewritten.trim());
                return rewritten.trim();
            }
        } catch (Exception e) {
            log.warn("Query rewriting failed, using original query: {}", e.getMessage());
        }
        return originalQuery;
    }

    private String resolveConversationId(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext().containsKey("conversationId")) {
            return (String) toolContext.getContext().get("conversationId");
        }
        return null;
    }

    private static String truncateText(String text, int maxChars) {
        if (text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "...";
    }

    private KnowledgeResult toKnowledgeResult(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        return new KnowledgeResult(
                doc.getText(),
                (String) metadata.get("sourceName"),
                (String) metadata.get("sourceUrl"),
                (String) metadata.get("sourceFormat")
        );
    }

    @Override
    public String toolId() { return "knowledge-search"; }

    @Override
    public String displayName() { return "Knowledge Base"; }

    @Override
    public String category() { return "knowledge"; }

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

    @Override
    public List<SourceReference> extractSources(String rawJsonResult) {
        if (rawJsonResult == null || rawJsonResult.isBlank()) return List.of();
        try {
            List<KnowledgeResult> results = objectMapper.readValue(
                    rawJsonResult, new TypeReference<List<KnowledgeResult>>() {});
            return results.stream()
                    .map(r -> new SourceReference(
                            "knowledge",
                            r.sourceName(),
                            null,
                            null,
                            null,
                            r.sourceFormat(),
                            null))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to extract knowledge search sources: {}", e.getMessage());
            return List.of();
        }
    }

    public record KnowledgeResult(
            String content,
            String sourceName,
            String sourceUrl,
            String sourceFormat
    ) {}
}
