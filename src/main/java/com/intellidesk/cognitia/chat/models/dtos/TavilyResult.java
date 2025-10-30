package com.intellidesk.cognitia.chat.models.dtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class TavilyResult {
    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("content")
    private String content;

    @JsonProperty("score")
    private Double score;

    @JsonProperty("raw_content")
    private String rawContent;

    @JsonProperty("favicon")
    private String favicon;

}
