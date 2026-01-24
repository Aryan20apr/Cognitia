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

    @Value("${tavily.extract.url:https://api.tavily.com/extract}")
    private String extractApiUrl;

    @Value("${tavily.extract.chunks_per_source:4}")
    private int defaultChunksPerSource;

    @Value("${tavily.extract.timeout_seconds:30}")
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

        log.info("Tavily Extract - Starting extraction request with {} URL(s), query='{}', extractDepth='{}'", 
                urls != null ? urls.size() : 0, query, extractDepth);

        if (urls == null || urls.isEmpty()) {
            log.info("Tavily Extract - Validation failed: No URLs provided");
            return "Please provide at least one valid URL to extract content from.";
        }
        if (query == null || query.isBlank()) {
            log.info("Tavily Extract - Validation failed: Query is null or blank");
            return "Please provide a query to guide the extraction.";
        }

        List<String> validUrls = urls.stream()
                .filter(url -> url != null && (url.startsWith("http://") || url.startsWith("https://")))
                .collect(Collectors.toList());

        log.info("Tavily Extract - URL validation: {} valid URL(s) out of {} provided", validUrls.size(), urls.size());

        if (validUrls.isEmpty()) {
            log.info("Tavily Extract - Validation failed: No valid URLs after filtering");
            return "No valid URLs provided. URLs must start with http:// or https://";
        }

        String depth = (extractDepth != null && !extractDepth.isBlank()) ? extractDepth : "advanced";
        log.info("Tavily Extract - Using extraction depth: '{}'", depth);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("urls", validUrls);
        requestBody.put("query", query);
        requestBody.put("chunks_per_source", defaultChunksPerSource);
        requestBody.put("extract_depth", depth);

        log.info("Tavily Extract - Prepared request: urls={}, query='{}', chunks_per_source={}, extract_depth='{}'", 
                validUrls, query, defaultChunksPerSource, depth);

        try {
            log.info("Tavily Extract - Sending API request to {}", extractApiUrl);
            String rawResponse = webClient.post()
                    .uri(extractApiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(timeoutSeconds));

            log.info("Tavily Extract - Received API response, response length: {} characters", 
                    rawResponse != null ? rawResponse.length() : 0);
            return parseResponse(rawResponse);

        } catch (Exception e) {
            log.error("Tavily Extract API error: {}", e.getMessage());
            return ERROR_MESSAGE;
        }
    }

    private String parseResponse(String rawResponse) {

        log.info("Tavily Extract - Parsing response, raw response length: {} characters", 
                rawResponse != null ? rawResponse.length() : 0);
        if (rawResponse == null || rawResponse.isBlank()) {
            log.info("Tavily Extract - Response is null or blank, cannot parse");
            return ERROR_MESSAGE;
        }

        try {
            log.info("Tavily Extract - Deserializing response to TavilyExtractResponse");
            TavilyExtractResponse resp = objectMapper.readValue(rawResponse, TavilyExtractResponse.class);

            if (resp.getResults() == null || resp.getResults().isEmpty()) {
                log.info("Tavily Extract - Response contains no results");
                return ERROR_MESSAGE;
            }

            log.info("Tavily Extract - Processing {} result(s) from response", resp.getResults().size());
            List<String> extractedTexts = new ArrayList<>();
            for (var result : resp.getResults()) {
                log.info("Tavily Extract - Processing result for URL: {}", result.getUrl());
                StringBuilder content = new StringBuilder();
                content.append("## Source: ").append(result.getUrl()).append("\n\n");

                if (result.getTitle() != null && !result.getTitle().isBlank()) {
                    content.append("**Title:** ").append(result.getTitle()).append("\n\n");
                    log.info("Tavily Extract - Added title for URL {}: '{}'", result.getUrl(), result.getTitle());
                }

                if (result.getRawContent() != null && !result.getRawContent().isBlank()) {
                    content.append(result.getRawContent()).append("\n");
                    log.info("Tavily Extract - Added content for URL {}, content length: {} characters", 
                            result.getUrl(), result.getRawContent().length());
                } else {
                    log.info("Tavily Extract - No content available for URL {}", result.getUrl());
                }

                extractedTexts.add(content.toString());
            }

            String finalResult = extractedTexts.stream().collect(Collectors.joining("\n---\n\n"));
            log.info("Tavily Extract - Successfully extracted from {} URLs, final result length: {} characters", 
                    resp.getResults().size(), finalResult.length());
            return finalResult;

        } catch (Exception e) {
            log.error("Failed to parse Tavily response: {}", e.getMessage());
            return ERROR_MESSAGE;
        }
    }
}
