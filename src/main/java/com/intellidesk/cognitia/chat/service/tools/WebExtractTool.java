package com.intellidesk.cognitia.chat.service.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.chat.models.dtos.TavilyExtractResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Tool for extracting text from a web page using Tavily Extract API.
 */
@Component
@Slf4j
public class WebExtractTool {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tavily.api.key}")
    private String apiKey;

    @Value("${tavily.api.extract.url:https://api.tavily.com/extract}")
    private String extractApiUrl;

    @Value("${tavily.api.chunks_per_source:4}")
    private int defaultChunksPerSource;

    @Tool(description = "Extract and retrieve specific content from web pages using Tavily Extract API. Use this tool when you need to extract detailed information from specific URLs based on a query.", returnDirect = false)
    public String extractText(
            @ToolParam(description = "List of URLs to extract content from. Provide one or more valid web page URLs.") List<String> urls,
            @ToolParam(description = "The query or question that guides what content to extract from the URLs. This helps focus the extraction on relevant information.") String query,
            @ToolParam(description = "The extraction depth: 'basic' for quick extraction or 'advanced' for more thorough extraction. Defaults to 'advanced' if not specified.") String extractDepth) {
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URLs list cannot be null or empty");
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or blank");
        }

        String depth = (extractDepth != null && !extractDepth.isBlank()) ? extractDepth : "advanced";
        int chunksPerSource = defaultChunksPerSource;

        // Build the request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("urls", urls);
        requestBody.put("query", query);
        requestBody.put("chunks_per_source", chunksPerSource);
        requestBody.put("extract_depth", depth);
        requestBody.put("include_usage", true);

        try {
            log.info("Tavily Extract - Params: urls={}, query='{}', extractDepth='{}'", 
                    urls, query, extractDepth);
            log.info("Tavily Extract - Request Body: {}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody));
        } catch (Exception ex) {
            log.warn("Failed to serialize requestBody for logging", ex);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String rawResponse = restTemplate.postForObject(extractApiUrl, entity, String.class);

            log.info("Tavily Extract - Raw Response: {}", rawResponse);

            if (rawResponse == null) {
                log.error("Tavily Extract - Tavily returned null response for urls={}, query='{}'", 
                        urls, query);
                throw new RuntimeException("Tavily returned null response");
            }

            // Parse JSON response
            TavilyExtractResponse resp = objectMapper.readValue(rawResponse, TavilyExtractResponse.class);

            try {
                log.debug("Tavily Extract - Parsed Results: {}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resp.getResults()));
            } catch (Exception ex) {
                log.warn("Failed to serialize parsed results for logging", ex);
            }

            // Combine all extracted content into a single string
            if (resp.getResults() == null || resp.getResults().isEmpty()) {
                log.warn("Tavily Extract - No results returned for urls={}, query='{}'", 
                        urls, query);
                return "No content extracted from the provided URLs.";
            }

            List<String> extractedTexts = new ArrayList<>();
            for (var result : resp.getResults()) {
                StringBuilder urlContent = new StringBuilder();
                urlContent.append("URL: ").append(result.getUrl()).append("\n");
                
                if (result.getTitle() != null && !result.getTitle().isBlank()) {
                    urlContent.append("Title: ").append(result.getTitle()).append("\n");
                }
                
                if (result.getRawContent() != null && !result.getRawContent().isBlank()) {
                    urlContent.append("Content: ").append(result.getRawContent()).append("\n");
                }
                
                extractedTexts.add(urlContent.toString());
            }

            return extractedTexts.stream().collect(Collectors.joining("\n\n---\n\n"));

        } catch (Exception e) {
            log.error("Error calling Tavily Extract API: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling Tavily Extract API: " + e.getMessage(), e);
        }
    }
}
