package com.intellidesk.cognitia.chat.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolDescriptor(
    String id,
    String displayName,
    String description,
    String category,
    String source,
    boolean userSelectable
) {}
