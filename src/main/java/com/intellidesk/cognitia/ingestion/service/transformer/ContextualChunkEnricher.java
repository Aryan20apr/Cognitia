package com.intellidesk.cognitia.ingestion.service.transformer;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualChunkEnricher implements DocumentTransformer {

    public static final String METADATA_KEY = "_document_context";

    private final ChatClient chatClient;

    public ContextualChunkEnricher(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return documents;

        String serializedContext = (String) documents.getFirst().getMetadata().get(METADATA_KEY);
        if (serializedContext == null) {
            log.warn("No _document_context found in chunk metadata, skipping contextual enrichment");
            return documents;
        }

        DocumentContext docContext = DocumentContext.deserialize(serializedContext);
        String contextPromptPart = docContext.toPromptString();

        for (int i = 0; i < documents.size(); i++) {
            Document chunk = documents.get(i);
            try {
                String prevText = i > 0 ? truncate(documents.get(i - 1).getText(), 800) : "NONE";
                String nextText = i < documents.size() - 1 ? truncate(documents.get(i + 1).getText(), 800) : "NONE";
                String currentText = chunk.getText();

                String prompt = buildPrompt(contextPromptPart, prevText, currentText, nextText);

                String situatingStatement = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                if (situatingStatement != null && !situatingStatement.isBlank()) {
                    String enrichedContent = situatingStatement.trim() + "\n\n" + currentText;
                    chunk.getMetadata().put("original_content", currentText);
                    documents.set(i, new Document(enrichedContent, chunk.getMetadata()));
                }

                if ((i + 1) % 50 == 0) {
                    log.info("Contextual enrichment progress: {}/{} chunks", i + 1, documents.size());
                }
            } catch (Exception e) {
                log.warn("Contextual enrichment failed for chunk {}/{}, leaving unenriched: {}",
                        i + 1, documents.size(), e.getMessage());
            }
        }

        log.info("Contextual enrichment complete: {}/{} chunks processed", documents.size(), documents.size());
        return documents;
    }

    private String buildPrompt(String documentContext, String prevChunk, String currentChunk, String nextChunk) {
        return """
                Document: %s

                Previous chunk: %s
                Current chunk: %s
                Next chunk: %s

                Give a short context (2-3 sentences) to situate the current chunk within the document for search retrieval purposes. Answer only with the context and nothing else.
                """.formatted(documentContext, prevChunk, truncate(currentChunk, 1500), nextChunk);
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "...";
    }
}
