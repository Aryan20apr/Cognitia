package com.intellidesk.cognitia.chat.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.chat.models.dtos.ChatThreadDTO;
import com.intellidesk.cognitia.chat.models.dtos.CustomChatResponse;
import com.intellidesk.cognitia.chat.models.dtos.UserMessageDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.service.ChatService;
import com.intellidesk.cognitia.chat.service.ThreadLockService.ThreadLockStatus;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.utils.exceptionHandling.DuplicateRequestAlreadyProcessedException;
import com.intellidesk.cognitia.utils.exceptionHandling.DuplicateRequestInProgressException;
import com.intellidesk.cognitia.utils.exceptionHandling.ThreadBusyException;

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> createThread() {
        ChatThread chatThread = chatService.createNewThread();
        
        Map<String, Object> response = Map.of(
            "threadId", chatThread.getId().toString(),
            "title", chatThread.getTitle(),
            "createdAt", chatThread.getCreatedAt().toString()
        );
        ApiResponse<Map<String,Object>> apiResponse = new ApiResponse<>("Thread created successfully", true, response);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
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

    @Operation(summary = "Get a chat thread")
    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ApiResponse<ChatThreadDTO>> getThread(@PathVariable String threadId){
        ChatThreadDTO chatThread = chatService.getThread(threadId);

        return ResponseEntity.ok().body(new ApiResponse<>("Thread fetched successfully", true, chatThread));
    }


    @Operation(summary = "Get all chat threads")
    @GetMapping("/threads")
    public ResponseEntity<ApiResponse<List<ChatThreadDTO>>> getAllThreads(){
        log.info("[ChatController :: getAllThreads] Fetching all chat threads at time: {}",LocalDateTime.now());
        List<ChatThreadDTO> chatThreads = chatService.getAllThreads();
        ApiResponse<List<ChatThreadDTO>> apiResponse = new ApiResponse<>("Threads fetched successfully", true, chatThreads);
        return ResponseEntity.ok().body(apiResponse);
    }

    @Operation(summary = "Update thread")
    @PatchMapping("/threads/{threadId}")
    public ResponseEntity<ApiResponse<?>> updateThread(@PathVariable String threadId, @RequestBody ChatThreadDTO chatThreadDTO){
        chatService.updateThread(threadId, chatThreadDTO);
        return ResponseEntity.ok().body(new ApiResponse<>("Thread updated successfully", true, chatThreadDTO));
    }

    @Operation(summary = "Delete a chat thread")
    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<ApiResponse<?>> deleteThread(@PathVariable String threadId){
        chatService.deleteThread(threadId);
        return ResponseEntity.ok().body(new ApiResponse<>("Thread deleted successfully", true, Map.of("threadId", threadId)));
    }

    @Operation(summary = "Check thread lock status", description = "Check if a thread is currently busy processing a message. Use this before sending to show appropriate UI state.")
    @GetMapping("/threads/{threadId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getThreadStatus(@PathVariable String threadId) {
        ThreadLockStatus status = chatService.getThreadLockStatus(threadId);
        
        Map<String, Object> statusData = Map.of(
            "threadId", threadId,
            "isBusy", status.isBusy(),
            "queuePosition", status.getWaitingCount(),
            "canSendMessage", !status.isBusy()
        );
        
        return ResponseEntity.ok().body(new ApiResponse<>(
            status.isBusy() ? "Thread is currently processing a message" : "Thread is ready",
            true,
            statusData
        ));
    }

    @Operation(summary = "Stream a chat response")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<org.springframework.http.codec.ServerSentEvent<String>> streamResponse(
        @RequestBody UserMessageDTO userMessageDTO
    ){
        return Flux.defer(() -> {
            // if (userMessageDTO.getThreadId() == null) {
            //     ChatThread chatThread = chatService.createNewThread();
            //     userMessageDTO.setThreadId(chatThread.getId().toString());
            // }
            return chatService.streamUserMessage(userMessageDTO);
        })
        .onErrorResume(ThreadBusyException.class, e -> {
            log.warn("[ChatController] Thread busy: {} - queue position: {}", e.getThreadId(), e.getQueuePosition());
            String errorJson = String.format(
                "{\"type\":\"error\",\"code\":\"THREAD_BUSY\",\"message\":\"%s\",\"threadId\":\"%s\",\"queuePosition\":%d,\"retryable\":true}",
                escapeJson("Please wait for another response on this thread to complete"),
                escapeJson(e.getThreadId()),
                e.getQueuePosition()
            );
            return Flux.just(
                ServerSentEvent.<String>builder(errorJson).event("error").build(),
                ServerSentEvent.<String>builder("[DONE]").build()
            );
        })
        .onErrorResume(IllegalArgumentException.class, e -> {
            log.error("[ChatController] Invalid argument error: {}", e.getMessage());
            return Flux.just(
                ServerSentEvent.<String>builder(
                    "{\"type\":\"error\",\"code\":\"INVALID_ARGUMENT\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"
                ).event("error").build(),
                ServerSentEvent.<String>builder("[DONE]").build()
            );
        })
        .onErrorResume(DuplicateRequestAlreadyProcessedException.class, e -> {
            log.error("[ChatController] Duplicate request already processed: {}", e.getMessage());
            return Flux.just(
                ServerSentEvent.<String>builder(
                    "{\"type\":\"error\",\"code\":\"DUPLICATE_PROCESSED\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}"
                ).event("error").build(),
                ServerSentEvent.<String>builder("[DONE]").build()
            );
        })
        .onErrorResume(DuplicateRequestInProgressException.class, e -> {
            log.error("[ChatController] Duplicate request in progress: {}", e.getMessage());
            return Flux.just(
                ServerSentEvent.<String>builder(
                    "{\"type\":\"error\",\"code\":\"DUPLICATE_IN_PROGRESS\",\"message\":\"" + escapeJson(e.getMessage()) + "\",\"retryable\":false}"
                ).event("error").build(),
                ServerSentEvent.<String>builder("[DONE]").build()
            );
        })
        .onErrorResume(RuntimeException.class, e -> {
            log.error("[ChatController] Runtime error: {}", e.getMessage(), e);
            return Flux.just(
                ServerSentEvent.<String>builder(
                    "{\"type\":\"error\",\"code\":\"INTERNAL_ERROR\",\"message\":\"Internal server error\"}"
                ).event("error").build(),
                ServerSentEvent.<String>builder("[DONE]").build()
            );
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
