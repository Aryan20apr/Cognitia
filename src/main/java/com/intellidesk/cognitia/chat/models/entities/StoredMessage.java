package com.intellidesk.cognitia.chat.models.entities;

import java.time.Instant;
import java.util.Map;

import org.springframework.ai.chat.messages.MessageType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StoredMessage {
    private MessageType messageType; // "USER", "ASSISTANT", "SYSTEM", ...
    private String text;
    private Map<String, Object> metadata;
    private Instant timestamp;

    public StoredMessage() {}

    public StoredMessage(MessageType messageType, String text, Map<String,Object> metadata, Instant timestamp) {
        this.messageType = messageType;
        this.text = text;
        this.metadata = metadata;
        this.timestamp = timestamp;
    }
}