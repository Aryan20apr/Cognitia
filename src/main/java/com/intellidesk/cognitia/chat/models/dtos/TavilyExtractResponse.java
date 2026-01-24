package com.intellidesk.cognitia.chat.models.dtos;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TavilyExtractResponse {
    @JsonProperty("results")
    private List<TavilyExtractResult> results;

    @JsonProperty("failed_results")
    private List<Object> failedResults;

    @JsonProperty("response_time")
    private Double responseTime;

    @JsonProperty("usage")
    private Map<String, Object> usage;

    @JsonProperty("request_id")
    private String requestId;
}
