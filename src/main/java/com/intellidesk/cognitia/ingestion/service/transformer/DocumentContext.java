package com.intellidesk.cognitia.ingestion.service.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record DocumentContext(
    String title,
    String author,
    String date,
    String description,
    String entities,
    int pageCount,
    String departmentName,
    String classificationName
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String serialize() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return fallbackSerialize();
        }
    }

    public static DocumentContext deserialize(String json) {
        try {
            return MAPPER.readValue(json, DocumentContext.class);
        } catch (Exception e) {
            return new DocumentContext("Unknown", null, null, null, null, 0, null, null);
        }
    }

    private String fallbackSerialize() {
        return String.join("|",
                safe(title), safe(author), safe(date),
                safe(description), safe(entities), String.valueOf(pageCount));
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    public String toPromptString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"").append(safe(title)).append("\"");
        if (author != null && !author.isBlank()) sb.append(" by ").append(author);
        if (date != null && !date.isBlank()) sb.append(" (").append(date).append(")");
        if (description != null && !description.isBlank()) sb.append(". ").append(description);
        if (entities != null && !entities.isBlank()) sb.append("\nKey entities: ").append(entities);
        return sb.toString();
    }
}
