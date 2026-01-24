package com.intellidesk.cognitia.chat.service.tools;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.chat.models.dtos.TavilyExtractResponse;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool for extracting text from web pages using Tavily Extract API.
 */
@Component
@Slf4j
public class WebExtractTool {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebClient webClient;

    @Value("${tavily.api.key}")
    private String apiKey;

    @Value("${tavily.api.extract.url:https://api.tavily.com/extract}")
    private String extractApiUrl;

    @Value("${tavily.api.chunks_per_source:4}")
    private int defaultChunksPerSource;

    @Value("${tavily.api.timeout.seconds:30}")
    private int timeoutSeconds;

    private static final String ERROR_MESSAGE = "Could not extract content from the URL(s). The page may be inaccessible or protected.";

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(extractApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Tool(description = "Extract and retrieve specific content from web pages using Tavily Extract API. Use this tool when you need to extract detailed information from specific URLs based on a query.", returnDirect = false)
    public String extractText(
            @ToolParam(description = "List of URLs to extract content from. Provide one or more valid web page URLs.") List<String> urls,
            @ToolParam(description = "The query or question that guides what content to extract from the URLs.") String query,
            @ToolParam(description = "The extraction depth: 'basic' for quick extraction or 'advanced' for more thorough extraction. Defaults to 'advanced'.") String extractDepth) {

        if (urls == null || urls.isEmpty()) {
            return "Please provide at least one valid URL to extract content from.";
        }
        if (query == null || query.isBlank()) {
            return "Please provide a query to guide the extraction.";
        }

        List<String> validUrls = urls.stream()
                .filter(url -> url != null && (url.startsWith("http://") || url.startsWith("https://")))
                .collect(Collectors.toList());

        if (validUrls.isEmpty()) {
            return "No valid URLs provided. URLs must start with http:// or https://";
        }

        String depth = (extractDepth != null && !extractDepth.isBlank()) ? extractDepth : "advanced";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("urls", validUrls);
        requestBody.put("query", query);
        requestBody.put("chunks_per_source", defaultChunksPerSource);
        requestBody.put("extract_depth", depth);

        log.info("Tavily Extract - urls={}, query='{}'", validUrls, query);

        try {
            String rawResponse = webClient.post()
                    .uri(extractApiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(timeoutSeconds));

            return parseResponse(rawResponse);

        } catch (Exception e) {
            log.error("Tavily Extract API error: {}", e.getMessage());
            return ERROR_MESSAGE;
        }
    }

    private String parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return ERROR_MESSAGE;
        }

        try {
            TavilyExtractResponse resp = objectMapper.readValue(rawResponse, TavilyExtractResponse.class);

            if (resp.getResults() == null || resp.getResults().isEmpty()) {
                return ERROR_MESSAGE;
            }

            List<String> extractedTexts = new ArrayList<>();
            for (var result : resp.getResults()) {
                StringBuilder content = new StringBuilder();
                content.append("## Source: ").append(result.getUrl()).append("\n\n");

                if (result.getTitle() != null && !result.getTitle().isBlank()) {
                    content.append("**Title:** ").append(result.getTitle()).append("\n\n");
                }

                if (result.getRawContent() != null && !result.getRawContent().isBlank()) {
                    content.append(result.getRawContent()).append("\n");
                }

                extractedTexts.add(content.toString());
            }

            log.info("Tavily Extract - Successfully extracted from {} URLs", resp.getResults().size());
            return extractedTexts.stream().collect(Collectors.joining("\n---\n\n"));

        } catch (Exception e) {
            log.error("Failed to parse Tavily response: {}", e.getMessage());
            return ERROR_MESSAGE;
        }
    }
}
