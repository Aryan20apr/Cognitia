package com.intellidesk.cognitia.chat.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.chat.models.dtos.CustomChatResponse;
import com.intellidesk.cognitia.chat.models.dtos.UserMessageDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@AllArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat endpoints")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Create a new chat thread")
    @PostMapping("/threads")
    public ResponseEntity<Map<String, Object>> createThread() {
        ChatThread chatThread = chatService.createNewThread();
        
        Map<String, Object> response = Map.of(
            "threadId", chatThread.getId().toString(),
            "title", chatThread.getTitle(),
            "createdAt", chatThread.getCreatedAt().toString()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "Post a user message and get a chat response")
    @PostMapping
    public ResponseEntity<?> postUserMessage(@RequestBody UserMessageDTO userMessageDTO){

        if(userMessageDTO.getThreadId() == null ){
            ChatThread chatThread = chatService.createNewThread();
            userMessageDTO.setThreadId(chatThread.getId().toString());
        }

        CustomChatResponse chatResponse =  chatService.processUserMessage(userMessageDTO);

        return ResponseEntity.ok().body(chatResponse);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamResponse(@RequestBody UserMessageDTO userMessageDTO){
        // Wrap in Flux.defer() to handle errors within the reactive pipeline
        return Flux.defer(() -> {
            if(userMessageDTO.getThreadId() == null ){
                ChatThread chatThread = chatService.createNewThread();
                userMessageDTO.setThreadId(chatThread.getId().toString());
            }
            return chatService.streamUserMessage(userMessageDTO);
        })
        .onErrorResume(IllegalArgumentException.class, e -> {
            log.error("[ChatController] Invalid argument error: {}", e.getMessage());
            return Flux.just("data: {\"error\": \"Bad Request\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}\n\n");
        })
        .onErrorResume(RuntimeException.class, e -> {
            log.error("[ChatController] Runtime error: {}", e.getMessage(), e);
            return Flux.just("data: {\"error\": \"Error\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}\n\n");
        })
        .onErrorResume(Exception.class, e -> {
            log.error("[ChatController] Unexpected error: {}", e.getMessage(), e);
            return Flux.just("data: {\"error\": \"Internal Server Error\", \"message\": \"An unexpected error occurred\"}\n\n");
        });
    }

    /**
     * Escapes special characters for JSON string values
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

}
