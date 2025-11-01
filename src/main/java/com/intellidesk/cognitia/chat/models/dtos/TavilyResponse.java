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
public class TavilyResponse {
    @JsonProperty("results")
    private List<TavilyResult> results;

    @JsonProperty("response_time")
    private Integer responseTime;

    @JsonProperty("auto_parameters")
    private Map<String, Object> autoParameters;

    @JsonProperty("request_id")
    private String requestId;

    public List<TavilyResult> getResults() {
        return results;
    }

    public void setResults(List<TavilyResult> results) {
        this.results = results;
    }
}

