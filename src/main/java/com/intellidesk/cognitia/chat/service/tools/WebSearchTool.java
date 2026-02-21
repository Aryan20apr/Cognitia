package com.intellidesk.cognitia.chat.service.tools;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.chat.models.dtos.TavilyResponse;
import com.intellidesk.cognitia.chat.models.dtos.TavilyResult;

import lombok.extern.slf4j.Slf4j;

/**
 * Tool for performing web search via the Tavily Search API.
 */
@Component
@Slf4j
public class WebSearchTool implements TimelineAwareTool {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tavily.api.url}")
    private String apiUrl;

    @Value("${tavily.api.key}")
    private String apiKey;

    @Value("${tavily.api.max_results}")
    private int defaultMaxResults;

    @Value("${tavily.api.topic}")
    private String defaultTopic;

    @Tool(description = "Perform a web search via Tavily and return top results with snippet and URLs", returnDirect = false)
    public List<TavilyResult> searchWeb(
            @ToolParam(description = "The search query string") String query,
            @ToolParam(description = "The maximum number of results to return") Integer maxResults,
            @ToolParam(description = "The search topic or category: can be 'general', 'news' or 'finance'") String topic) {
        int mr = (maxResults != null && maxResults > 0) ? maxResults : defaultMaxResults;
        String tp = (topic != null && !topic.isBlank()) ? topic : defaultTopic;

        // Build the request body or params depending on Tavily API spec
        Map<String, Object> requestBody = Map.of(
                "query", query,
                "max_results", mr,
                "topic", tp,
                "search_depth", "basic",
                "chunks_per_source", 3,
                "include_answer", "advanced",
                "include_raw_content", true,
                "include_images", false,
                "include_favicon", false);

        try {
            log.info("Tavily WebSearch - Params: query='{}', maxResults={}, topic='{}'", query, maxResults, topic);
            log.info("Tavily WebSearch - Request Body: {}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody));
        } catch (Exception ex) {
            log.warn("Failed to serialize requestBody for logging", ex);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String rawResponse = restTemplate.postForObject(apiUrl, entity, String.class);

            log.info("Tavily WebSearch - Raw Response: {}", rawResponse);

            if (rawResponse == null) {
                log.error(
                        "Tavily WebSearch - Tavily returned null response for params: query={}, maxResults={}, topic={}",
                        query, maxResults, topic);
                throw new RuntimeException("Tavily returned null response");
            }
            // Parse JSON
            TavilyResponse resp = objectMapper.readValue(rawResponse, TavilyResponse.class);

            try {
                log.debug("Tavily WebSearch - Parsed Results: {}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resp.getResults()));
            } catch (Exception ex) {
                log.warn("Failed to serialize parsed results for logging", ex);
            }

            return resp.getResults();
        } catch (Exception e) {
            log.error("Error calling Tavily Search API: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling Tavily Search API: " + e.getMessage(), e);
        }
    }

    @Override
    public String timelineDescription() {
        return "Searching the web";
    }

    @Override
    public String summarizeResult(String rawJsonResult) {
        if (rawJsonResult == null || rawJsonResult.isBlank()) return "No results";
        try {
            List<?> results = objectMapper.readValue(rawJsonResult, List.class);
            int count = results.size();
            return "Found " + count + " search result" + (count != 1 ? "s" : "");
        } catch (Exception e) {
            return "Search completed";
        }
    }
}
