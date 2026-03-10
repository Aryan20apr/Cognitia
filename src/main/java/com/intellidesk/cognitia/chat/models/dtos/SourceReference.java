package com.intellidesk.cognitia.chat.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SourceReference(
    String type,
    String title,
    String url,
    String favicon,
    String sourceId,
    String format,
    Double score
) {}
