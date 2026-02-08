package com.intellidesk.cognitia.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.repository.ChatThreadRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ThreadTitleGenerationService {

    private final ChatThreadRepository threadRepository;
    private final ChatClient chatClient;

    
    public ThreadTitleGenerationService(
            ChatThreadRepository threadRepository,
            @Qualifier("lightClient") ChatClient chatClient) {
        this.threadRepository = threadRepository;
        this.chatClient = chatClient;
    }

    private static final String DEFAULT_TITLE = "New Chat";
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_RESPONSE_FOR_TITLE = 500;

    @Async
    public void generateTitleIfNeeded(ChatThread thread, String userMessage, String assistantResponse) {
        try {
            
            ChatThread currentThread = threadRepository.findById(thread.getId()).orElse(null);
            if (currentThread == null) {
                log.warn("[TitleGeneration] Thread {} not found", thread.getId());
                return;
            }
            if (!shouldGenerateTitle(currentThread)) {
                log.debug("[TitleGeneration] Thread {} already has custom title: {}", 
                    thread.getId(), currentThread.getTitle());
                return;
            }

            log.info("[TitleGeneration] Generating title for thread {}", thread.getId());

            String generatedTitle = generateTitle(userMessage, assistantResponse);

            generatedTitle = sanitizeTitle(generatedTitle, userMessage);

            currentThread.setTitle(generatedTitle);
            threadRepository.save(currentThread);
            
            log.info("[TitleGeneration] Updated thread {} title to: {}", thread.getId(), generatedTitle);

        } catch (Exception e) {
            log.warn("[TitleGeneration] Failed to generate title for thread {}: {}, stack: {}", 
                thread.getId(), e.getMessage(), e.getStackTrace());
            // Silently fail - user can still manually rename
        }
    }

    private boolean shouldGenerateTitle(ChatThread thread) {
        String title = thread.getTitle();
        return title == null 
            || title.isEmpty() 
            || title.equals(DEFAULT_TITLE)
            || title.equalsIgnoreCase("new chat");
    }

    private String generateTitle(String userMessage, String assistantResponse) {
        String truncatedResponse = assistantResponse.length() > MAX_RESPONSE_FOR_TITLE 
            ? assistantResponse.substring(0, MAX_RESPONSE_FOR_TITLE) 
            : assistantResponse;

        String prompt = String.format(
            "User: %s\nAssistant: %s",
            userMessage,
            truncatedResponse
        );
        
        log.info("[generateTitle] prompt: {}",prompt);

        String systemPrompt = """
            You are a title generator. Given the following conversation, generate a concise title 
            in 6 words or fewer. The title should capture the main topic or intent.
            Do NOT use quotes. Do NOT add punctuation at the end.
            Respond with ONLY the title, nothing else.
            """;

        String title = chatClient.prompt()
            .system(systemPrompt)
            .user(prompt)
            .call()
            .content();
        
        log.info("[generateTitle] title: {}",title);

        return title != null ? title.trim() : "";
    }

    private String sanitizeTitle(String title, String userMessageFallback) {
        if (title == null) {
            title = "";
        }

        title = title.replaceAll("[\"']", "").trim();

        title = title.replaceAll("[.!?]+$", "");

        if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
            title = userMessageFallback.length() > 50 
                ? userMessageFallback.substring(0, 50).trim() + "..." 
                : userMessageFallback.trim();
        }
        
        return title;
    }
}