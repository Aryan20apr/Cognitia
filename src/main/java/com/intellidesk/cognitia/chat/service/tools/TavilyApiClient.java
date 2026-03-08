package com.intellidesk.cognitia.chat.service.tools;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TavilyApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private WebClient webClient;

    @Value("${tavily.api.key}")
    private String apiKey;

    @Value("${tavily.extract.url:https://api.tavily.com/extract}")
    private String extractApiUrl;

    @Value("${tavily.extract.timeout_seconds:30}")
    private int extractTimeoutSeconds;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(extractApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Retryable(
        retryFor = { RestClientException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public String search(String apiUrl, HttpEntity<Map<String, Object>> entity) {
        log.debug("[TavilyApiClient] Calling search API: {}", apiUrl);
        return restTemplate.postForObject(apiUrl, entity, String.class);
    }

    @Retryable(
        retryFor = { WebClientResponseException.class, RestClientException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public String extract(String extractUrl, Map<String, Object> requestBody) {
        log.debug("[TavilyApiClient] Calling extract API: {}", extractUrl);
        return webClient.post()
                .uri(extractUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(extractTimeoutSeconds));
    }
}
