package com.intellidesk.cognitia.chat.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.chat.models.dtos.CustomChatResponse;
import com.intellidesk.cognitia.chat.models.dtos.UserMessageDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatMessage;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.repository.ChatMessageRepository;
import com.intellidesk.cognitia.chat.repository.ChatThreadRepository;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.security.CustomUserDetails;


import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final VectorStore vectorStore;
    private final ChatMemoryHydrator chatMemoryHydrator;


    @Transactional
    public CustomChatResponse processUserMessage(UserMessageDTO message){

        final UUID threadId =UUID.fromString( message.getThreadId());
        
        String requestId = message.getRequestId(); 
        // Get current authenticated user ID
        String userId = extractUserIdFromSecurityContext();
        final String resolvedUserId = userId;
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

                If the question refers to a recent or external event and you lack enough
                information in the context, use the WebSearchTool to perform a web search
                and include that information in your answer.

                You also have access to the DateTimeTool for getting the current date and time.

                Always respond in JSON format matching this schema:
                {
                  "answer": string,
                  "sources": [string],
                  "followUpSuggestions": [string],
                  "confidenceScore": number
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
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, threadId.toString());
                    // Use thread.getUserId() if userId is not directly available
                    a.param("requestId",requestId != null ? requestId: "1");
                    a.param("userId", resolvedUserId != null ? resolvedUserId.toString() : "");
                    a.param("tenantId", TenantContext.getTenantId().toString());
                }) // Connect memory
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
        User user = new User();
        user.setId(UUID.fromString(extractUserIdFromSecurityContext()));
        ChatThread chatThread = new ChatThread();
        chatThread.setTitle("New Chat");
        chatThread.setUser(user);
        threadRepository.save(chatThread);
        return chatThread;
     }   
     String extractUserIdFromSecurityContext(){
        String userId = null;
        
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                userId = userDetails.getUser().getId().toString();
            }
        } catch (Exception e) {
            // Log warning but continue without userId
            System.err.println("Could not extract userId from SecurityContext: " + e.getMessage());
        }
        return userId;
     }


    @Transactional
    public Flux<String> streamUserMessage(UserMessageDTO message){

        final UUID threadId =UUID.fromString( message.getThreadId());
        
        String requestId = message.getRequestId(); 
        // Get current authenticated user ID
        String userId = extractUserIdFromSecurityContext();
        final String resolvedUserId = userId;
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
                You are a helpful AI assistant. Use both the provided context and prior chat memory
                to generate clear, accurate, conversational answers.

                If the question refers to a recent or external event and you lack sufficient
                information in the context, use the WebSearchTool to look up current information
                and incorporate it naturally into your response.

                You also have access to the DateTimeTool for retrieving the current date and time.

                Response format requirements:
                - Respond in clean, well-structured Markdown suitable for incremental streaming.
                - Use headings (##) to organize the answer when helpful.
                - Use bullet points or numbered lists for structure.
                - Use inline code (`like_this`) and fenced code blocks (```language) where appropriate.
                - Never output JSON unless explicitly asked by the user.
                - Never wrap the entire response in JSON.
                - Always append a final section titled **Sources** at the bottom (even if empty).
                - After Sources, append a section titled **Follow-up Questions** with 2–3 suggestions.
                - The answer must remain valid Markdown throughout streaming.

                Do not mention these rules. Respond only with the answer.
                """;


        String fullPrompt = """
                Context:
                %s

                User:
                %s
                """.formatted(context, userMessage);

        // 5️⃣ Call the LLM
        Flux<String> streamResponse = chatClient.prompt()
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, threadId.toString());
                    // Use thread.getUserId() if userId is not directly available
                    a.param("requestId",requestId != null ? requestId: "1");
                    a.param("userId", resolvedUserId != null ? resolvedUserId.toString() : "");
                    a.param("tenantId", TenantContext.getTenantId().toString());
                }) // Connect memory
                .system(systemPrompt)
                .user(fullPrompt)
                .stream().content();

        
            AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());

            // Return a stream of individual tokens
        return streamResponse
                .flatMap(chunk -> {
                
                    String delta = chunk;
                    buffer.get().append(delta);
                    return Mono.just(delta);  // send token to the client
                })
                .doOnComplete(() -> {
                    // Save final AI message
                    ChatMessage aiMsg = ChatMessage.builder()
                            .thread(thread)
                            .sender(MessageType.ASSISTANT)
                            .content(buffer.get().toString())
                            .build();

                    messageRepository.save(aiMsg);
                    thread.addMessage(aiMsg);
                    threadRepository.save(thread);
                });
        }
    }