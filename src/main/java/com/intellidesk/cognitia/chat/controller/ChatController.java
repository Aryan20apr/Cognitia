package com.intellidesk.cognitia.chat.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@AllArgsConstructor
@Tag(name = "Chat", description = "Chat endpoints")
public class ChatController {

    private ChatService chatService;
    
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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamResponse(@RequestBody UserMessageDTO userMessageDTO){
         if(userMessageDTO.getThreadId() == null ){
            ChatThread chatThread = chatService.createNewThread();
            userMessageDTO.setThreadId(chatThread.getId().toString());
        }

        return chatService.streamUserMessage(userMessageDTO);
    }

}
