package com.intellidesk.cognitia.chat.service;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.chat.models.dtos.CustomChatResponse;
import com.intellidesk.cognitia.chat.models.dtos.UserMessageDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatMessage;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.repository.ChatMessageRepository;
import com.intellidesk.cognitia.chat.repository.ChatThreadRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final VectorStore vectorStore;
    private final SimpleChatMemoryService simpleChatMemoryService;
    private final ChatMemoryHydrator chatMemoryHydrator;


    @Transactional
    public CustomChatResponse processUserMessage(UserMessageDTO message){

        final UUID threadId =UUID.fromString( message.getThreadId());

        ChatThread thread = threadRepository.findById(threadId)
        .orElseThrow(() -> new RuntimeException("Thread not found"));

        String userMessage = message.getMessage();

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(userMessage).similarityThreshold(0.6d).topK(3).build());

        String context = documents.stream()
        .map(Document::getFormattedContent)
        .reduce("", (a, b) -> a + "\n" + b);

        // 2️⃣ Persist user message
        ChatMessage userMsg = ChatMessage.builder()
                .thread(thread)
                .sender(MessageType.USER)
                .content(userMessage)
                .build();
        messageRepository.save(userMsg);

        thread.addMessage(userMsg);

        // String history = simpleChatMemoryService.loadMemoryForThread(threadId);
        chatMemoryHydrator.hydrateIfEmpty(thread.getId().toString());
       
        String systemPrompt = """
                 You are a helpful assistant. Use both context and prior chat memory
                to generate clear and accurate answers.

                Always respond in JSON format matching this schema:
                {
                  "answer": string,
                  "sources": [string],
                  "followUpSuggestions": [string],
                  "confidenceScore": number
                }

                 If you do not find relevant information in the context, respond with:
                {
                    "answer": "I do not have an answer to this question.",
                    "sources": [],
                    "followUpSuggestions": [],
                    "confidenceScore": 0
                }
                """;

        String fullPrompt = """
                Context:
                %s

                User:
                %s
                """.formatted(context, userMessage);

        // 5️⃣ Call the LLM
        CustomChatResponse customChatResponse = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, threadId.toString())) // Connect memory
                .system(systemPrompt)
                .user(fullPrompt)
                .call()
                .entity(CustomChatResponse.class);
    
            // 5️⃣ Save AI response
            ChatMessage aiMsg = ChatMessage.builder()
                    .thread(thread)
                    .sender(MessageType.ASSISTANT)
                    .content(customChatResponse.getAnswer())
                    .build();
            messageRepository.save(aiMsg);

            thread.addMessage(aiMsg);
            threadRepository.save(thread);
    
            return customChatResponse;
        }

     public ChatThread createNewThread(){

        ChatThread chatThread = new ChatThread();
        chatThread.setTitle("New Chat");
        threadRepository.save(chatThread);
        return chatThread;
     }   

    }