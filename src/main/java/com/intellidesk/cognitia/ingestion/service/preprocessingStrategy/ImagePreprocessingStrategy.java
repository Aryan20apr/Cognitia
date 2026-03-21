package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import com.intellidesk.cognitia.analytics.service.MeteringIngestService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ImagePreprocessingStrategy implements PreprocessingStrategy {

    private final ChatModel chatModel;
    private final MeteringIngestService meteringIngestService;

    private static final String VISION_PROMPT =
            "Describe everything visible in this image in detail. "
            + "Include all text, labels, numbers, diagram structures, chart data, "
            + "table contents, and visual elements. "
            + "Be thorough and structured in your description.";

    public ImagePreprocessingStrategy(ChatModel chatModel, MeteringIngestService meteringIngestService) {
        this.chatModel = chatModel;
        this.meteringIngestService = meteringIngestService;
    }

    @Override
    public List<Document> preprocess(Resource resource,
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {

        log.info("Starting image preprocessing for resource: {}", rawSource.getResId());

        try {
            String mimeType = resolveMimeType(rawSource.getFormat());

            var userMessage = UserMessage.builder()
                    .text(VISION_PROMPT)
                    .media(List.of(new Media(MimeType.valueOf(mimeType), resource)))
                    .build();

            ChatResponse response = chatModel.call(new Prompt(userMessage));
            String description = response.getResult().getOutput().getText();

            log.info("Image description generated for resource {}, length: {} chars",
                    rawSource.getResId(), description != null ? description.length() : 0);

            trackUsage(response, rawSource.getTenantId(), rawSource.getResId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("tenantId", rawSource.getTenantId().toString());
            metadata.put("sourceId", rawSource.getResId().toString());
            metadata.put("sourceName", rawSource.getName());
            metadata.put("sourceUrl", rawSource.getUrl());
            metadata.put("sourceFormat", rawSource.getFormat());
            metadata.put("imageDescription", true);
            metadata.put("fileName", rawSource.getName());
            metadata.put("ingestionTimestamp", Instant.now().toString());
            metadata.put("contentType", mimeType);

            return List.of(new Document(description, metadata));

        } catch (Exception e) {
            log.error("Image preprocessing failed for resource {}: {}", rawSource.getResId(), e.getMessage(), e);
            throw new RuntimeException("Failed to preprocess image: " + e.getMessage(), e);
        }
    }

    private void trackUsage(ChatResponse response, UUID tenantId, UUID resourceId) {
        try {
            if (response.getMetadata() == null || response.getMetadata().getUsage() == null) {
                log.warn("No usage metadata in vision response for resource {}", resourceId);
                return;
            }
            var usage = response.getMetadata().getUsage();
            String model = response.getMetadata().getModel();
            Long promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : 0L;
            Long completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().longValue() : 0L;
            Long totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : promptTokens + completionTokens;

            String requestId = "img-ingest-" + resourceId;

            meteringIngestService.recordUsage(
                    tenantId, null, null, requestId,
                    model, promptTokens, completionTokens, totalTokens,
                    "{\"source\":\"image-ingestion\",\"resourceId\":\"" + resourceId + "\"}");

            log.info("Tracked image ingestion usage for tenant {}: {} total tokens (model: {})",
                    tenantId, totalTokens, model);
        } catch (Exception e) {
            log.warn("Failed to track image ingestion usage for resource {}: {}", resourceId, e.getMessage());
        }
    }

    private String resolveMimeType(String format) {
        String ext = normalizeFormat(format);
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }

    private String normalizeFormat(String format) {
        if (format == null) return "";
        String trimmed = format.trim().toLowerCase();
        return trimmed.startsWith(".") ? trimmed.substring(1) : trimmed;
    }
}
