package com.intellidesk.cognitia.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.repository.ChatThreadRepository;
import java.time.LocalDateTime;
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
            String title = generateTitleBlocking(thread, userMessage, assistantResponse);
            if (title != null) {
                persistTitle(thread.getId(), title);
            }
        } catch (Exception e) {
            log.warn("[TitleGeneration] Failed to generate title for thread {}: {}", 
                thread.getId(), e.getMessage());
        }
    }

    /**
     * Generates a title synchronously (blocking). Returns the sanitized title string,
     * or null if title generation should be skipped (thread already has a custom title).
     * Does NOT persist the title — caller is responsible for saving.
     */
    public String generateTitleBlocking(ChatThread thread, String userMessage, String assistantResponse) {
        ChatThread currentThread = threadRepository.findById(thread.getId()).orElse(null);
        if (currentThread == null) {
            log.warn("[TitleGeneration] Thread {} not found", thread.getId());
            return null;
        }
        if (!shouldGenerateTitle(currentThread)) {
            log.debug("[TitleGeneration] Thread {} already has custom title: {}", 
                thread.getId(), currentThread.getTitle());
            return null;
        }

        log.info("[TitleGeneration] Generating title for thread {}", thread.getId());

        String generatedTitle = generateTitle(userMessage, assistantResponse);
        return sanitizeTitle(generatedTitle, userMessage);
    }

    /**
     * Persists a generated title to the database.
     */
    public void persistTitle(java.util.UUID threadId, String title) {
        ChatThread thread = threadRepository.findById(threadId).orElse(null);
        if (thread == null) return;
        thread.setTitle(title);
        threadRepository.save(thread);
        log.info("[TitleGeneration] Updated thread {} title to: {}", threadId, title);
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
            
            """;

        String title = chatClient.prompt()
            .system(systemPrompt)
            .user(prompt)
            .call()
            .content();
        
        log.info("[generateTitle] title: {} at time: {}",title,LocalDateTime.now());

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