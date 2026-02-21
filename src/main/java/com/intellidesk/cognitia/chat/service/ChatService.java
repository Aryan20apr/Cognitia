package com.intellidesk.cognitia.chat.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.chat.models.dtos.AgentStep;
import com.intellidesk.cognitia.chat.models.dtos.ChatMessageDTO;
import com.intellidesk.cognitia.chat.models.dtos.ChatThreadDTO;
import com.intellidesk.cognitia.chat.models.dtos.CustomChatResponse;
import com.intellidesk.cognitia.chat.models.dtos.UserMessageDTO;
import com.intellidesk.cognitia.chat.models.entities.ChatMessage;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.chat.repository.ChatMessageRepository;
import com.intellidesk.cognitia.chat.repository.ChatThreadRepository;
import com.intellidesk.cognitia.chat.service.ThreadLockService.ThreadLockStatus;
import com.intellidesk.cognitia.chat.service.tools.TimelineToolCallbackProvider;
import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;
import com.intellidesk.cognitia.userandauth.security.CustomUserDetails;
import com.intellidesk.cognitia.utils.exceptionHandling.ThreadBusyException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class ChatService {

    private final ChatClient chatClient;
    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final VectorStore vectorStore;
    private final ChatMemoryHydrator chatMemoryHydrator;
    private final ThreadLockService threadLockService;
    private final ThreadTitleGenerationService titleGenerationService;
    private final TimelineToolCallbackProvider timelineToolCallbackProvider;

    @Transactional
    public ChatThreadDTO getThread(String threadId) {
        ChatThread thread = threadRepository.findById(UUID.fromString(threadId))
                .orElseThrow(() -> new RuntimeException("Thread not found"));

        return ChatThreadDTO.builder()
                .id(thread.getId())
                .title(thread.getTitle())
                .userId(thread.getUser() != null ? thread.getUser().getId() : null)
                .createdAt(thread.getCreatedAt())
                .messages(thread.getMessages().stream()
                        .map(message -> ChatMessageDTO.builder()
                                .id(message.getId())
                                .content(message.getContent())
                                .role(message.getSender() != null ? message.getSender().name() : null)
                                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toInstant() : null)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public Boolean updateThread(String threadId, ChatThreadDTO chatThreadDTO) {
        UUID id = UUID.fromString(threadId);

        int rowsUpdated = threadRepository.updateTitleById(id, chatThreadDTO.getTitle());

        if (rowsUpdated == 0) {
            throw new RuntimeException("Thread not found");
        }

        return true;
    }

    @Transactional
    public List<ChatThreadDTO> getAllThreads() {
        return threadRepository.findAll().stream()
                .map(thread -> ChatThreadDTO.builder()
                        .id(thread.getId())
                        .title(thread.getTitle())
                        .createdAt(thread.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteThread(String threadId) {
        if (threadRepository.existsById(UUID.fromString(threadId)))
            threadRepository.deleteById(UUID.fromString(threadId));
    }

    /**
     * Get the lock status for a thread.
     * Useful for clients to check if they should wait before sending.
     */
    public ThreadLockStatus getThreadLockStatus(String threadId) {
        return threadLockService.getStatus(UUID.fromString(threadId));
    }

    @Transactional
    public CustomChatResponse processUserMessage(UserMessageDTO message) {

        final UUID threadId = UUID.fromString(message.getThreadId());

        String lockToken = threadLockService.tryAcquire(threadId);
        if (lockToken == null) {
            ThreadLockStatus status = threadLockService.getStatus(threadId);
            throw new ThreadBusyException(message.getThreadId(), status.queuePosition());
        }

        try {
            String requestId = message.getRequestId();
            String userId = extractUserIdFromSecurityContext();
            final String resolvedUserId = userId;
            ChatThread thread = threadRepository.findById(threadId)
                    .orElseThrow(() -> new RuntimeException("Thread not found"));

            String userMessage = message.getMessage();

            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder().query(userMessage).similarityThreshold(0.6d).topK(3).build());

            String context = documents.stream()
                    .map(Document::getFormattedContent)
                    .reduce("", (a, b) -> a + "\n" + b);

            ChatMessage userMsg = ChatMessage.builder()
                    .thread(thread)
                    .sender(MessageType.USER)
                    .content(userMessage)
                    .build();
            messageRepository.save(userMsg);

            thread.addMessage(userMsg);

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

        CustomChatResponse customChatResponse = chatClient.prompt()
                .advisors(a -> {
                    a.param(ChatMemory.CONVERSATION_ID, threadId.toString());
                    a.param(Constants.PARAM_REQUEST_ID, requestId != null ? requestId : UUID.randomUUID().toString());
                    a.param(Constants.PARAM_USER_ID, resolvedUserId != null ? resolvedUserId : "");
                    a.param(Constants.PARAM_TENANT_ID, TenantContext.getTenantId().toString());
                })
                .system(systemPrompt)
                .user(fullPrompt)
                .toolCallbacks(timelineToolCallbackProvider.createAugmentedToolCallbacks(null))
                .call()
                .entity(CustomChatResponse.class);

            String answer = customChatResponse != null ? customChatResponse.getAnswer() : "";
            ChatMessage aiMsg = ChatMessage.builder()
                    .thread(thread)
                    .sender(MessageType.ASSISTANT)
                    .content(answer != null ? answer : "")
                    .build();
            messageRepository.save(aiMsg);

            thread.addMessage(aiMsg);
            threadRepository.save(thread);

            return customChatResponse;
        } finally {
            threadLockService.release(threadId, lockToken);
        }
    }

    public ChatThread createNewThread() {
        User user = new User();
        user.setId(UUID.fromString(extractUserIdFromSecurityContext()));
        ChatThread chatThread = new ChatThread();
        chatThread.setTitle("New Chat");
        chatThread.setUser(user);
        threadRepository.save(chatThread);
        return chatThread;
    }

    String extractUserIdFromSecurityContext() {
        String userId = null;

        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                userId = userDetails.getUser().getId().toString();
            }
        } catch (Exception e) {
            log.warn("Could not extract userId from SecurityContext: {}", e.getMessage());
        }
        return userId;
    }

    /**
     * Context holder for streaming chat data
     */
    private record StreamContext(
            ChatThread thread,
            String context,
            String userMessage,
            String requestId,
            String userId,
            UUID threadId,
            String lockToken) {
    }

    @Transactional
    public Flux<ServerSentEvent<String>> streamUserMessage(UserMessageDTO message) {
        final UUID threadId = UUID.fromString(message.getThreadId());

        // Try to acquire thread lock BEFORE starting the reactive chain
        String lockToken = threadLockService.tryAcquire(threadId);
        if (lockToken == null) {
            ThreadLockStatus status = threadLockService.getStatus(threadId);
            log.info("[ChatService] Thread {} is busy, queue position: {}", threadId, status.queuePosition());
            throw new ThreadBusyException(message.getThreadId(), status.queuePosition());
        }

        log.info("[ChatService] Lock acquired for thread {}, starting stream", threadId);

        // Wrap synchronous setup in Mono.fromCallable for proper reactive error
        // handling
        return Mono.fromCallable(() -> {
            String requestId = message.getRequestId();
            // Get current authenticated user ID
            String userId = extractUserIdFromSecurityContext();

            ChatThread thread = threadRepository.findById(threadId)
                    .orElseThrow(() -> new RuntimeException("Thread not found"));

            String userMessage = message.getMessage();

            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(userMessage)
                            .similarityThreshold(0.6d)
                            .topK(3)
                            .build());

            String context = documents.stream()
                    .map(Document::getFormattedContent)
                    .reduce("", (a, b) -> a + "\n" + b);

            // Persist user message
            ChatMessage userMsg = ChatMessage.builder()
                    .thread(thread)
                    .sender(MessageType.USER)
                    .content(userMessage)
                    .build();
            messageRepository.save(userMsg);

            thread.addMessage(userMsg);

            chatMemoryHydrator.hydrateIfEmpty(thread.getId().toString());

            return new StreamContext(thread, context, userMessage, requestId, userId, threadId, lockToken);
        })
                .flatMapMany(ctx -> {
                    AgentTimelineContext timeline = new AgentTimelineContext();

                    timeline.emitStep(AgentStep.thinking("Analyzing your question..."));

                    ToolCallback[] requestTools = timelineToolCallbackProvider
                        .createAugmentedToolCallbacks(timeline);

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
                            """.formatted(ctx.context(), ctx.userMessage());

                    AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
                    AtomicBoolean firstContentEmitted = new AtomicBoolean(false);

                    CompletableFuture<String> titleFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            return titleGenerationService.generateTitleBlocking(
                                    ctx.thread(), ctx.userMessage(), "");
                        } catch (Exception e) {
                            log.warn("[ChatService] Title generation failed: {}", e.getMessage());
                            return null;
                        }
                    });

                    Flux<ServerSentEvent<String>> contentStream = chatClient.prompt()
                            .advisors(a -> {
                                a.param(ChatMemory.CONVERSATION_ID, ctx.threadId().toString());
                                a.param("requestId",
                                        ctx.requestId() != null ? ctx.requestId() : UUID.randomUUID().toString());
                                a.param("userId", ctx.userId() != null ? ctx.userId() : "");
                                a.param("tenantId", TenantContext.getTenantId().toString());
                            })
                            .system(systemPrompt)
                            .user(fullPrompt)
                            .toolCallbacks(requestTools)
                            .stream().content()
                            .doOnNext(chunk -> {
                                buffer.get().append(chunk);
                                if (firstContentEmitted.compareAndSet(false, true)) {
                                    timeline.emitStep(AgentStep.generating("Writing response..."));
                                }
                            })
                            .transform(flux -> bufferByLineWithTimeout(flux, Duration.ofMillis(500), 500))
                            .doOnNext(batch -> log.debug("[ChatService] Streaming batch: {}", batch))
                            .map(batch -> ServerSentEvent.<String>builder(batch).build())
                            .doOnComplete(() -> {
                                ChatMessage aiMsg = ChatMessage.builder()
                                        .thread(ctx.thread())
                                        .sender(MessageType.ASSISTANT)
                                        .content(buffer.get().toString())
                                        .build();

                                messageRepository.save(aiMsg);
                                ctx.thread().addMessage(aiMsg);
                                threadRepository.save(ctx.thread());
                                log.info("[ChatService] Stream completed for thread {}", ctx.threadId());

                                timeline.complete();
                            })
                            .doFinally(signalType -> {
                                threadLockService.release(ctx.threadId(), ctx.lockToken());
                                log.info("[ChatService] Lock released for thread {} (signal: {})", ctx.threadId(),
                                        signalType);
                            });

                    return Flux.merge(timeline.steps(), contentStream)
                            .concatWith(Mono.defer(() -> {
                                try {
                                    String title = titleFuture.get(10, TimeUnit.SECONDS);
                                    if (title != null && !title.isEmpty()) {
                                        titleGenerationService.persistTitle(ctx.threadId(), title);
                                        log.info("[ChatService] Emitting title SSE for thread {}: {}", ctx.threadId(),
                                                title);
                                        return Mono.just(
                                                ServerSentEvent.<String>builder(title)
                                                        .event("thread-title")
                                                        .build());
                                    }
                                } catch (Exception e) {
                                    log.info(
                                            "[ChatService] Title not ready in time for thread {}, falling back to async",
                                            ctx.threadId());
                                    titleGenerationService.generateTitleIfNeeded(
                                            ctx.thread(), ctx.userMessage(), buffer.get().toString());
                                }
                                return Mono.empty();
                            }))
                            .concatWith(
                                    Mono.just(
                                            ServerSentEvent.<String>builder("[DONE]").build()));
                })
                .doOnError(e -> {
                    threadLockService.release(threadId, lockToken);
                    log.error("[ChatService] Error in stream for thread {}: {}", threadId, e.getMessage());
                });
    }

    /**
     * Buffers streaming tokens and emits when:
     * 1. A newline is detected (preserves markdown line structure)
     * 2. Timeout is reached (prevents long waits on paragraphs)
     * 3. Buffer size exceeds maxChars (memory safety)
     */
    private Flux<String> bufferByLineWithTimeout(Flux<String> source, Duration timeout, int maxChars) {
        return Flux.create(sink -> {
            StringBuilder lineBuffer = new StringBuilder();
            AtomicLong lastEmit = new AtomicLong(System.currentTimeMillis());

            source.subscribe(
                    chunk -> {
                        lineBuffer.append(chunk);
                        long now = System.currentTimeMillis();
                        boolean hasNewline = chunk.contains("\n");
                        boolean timeoutReached = (now - lastEmit.get()) > timeout.toMillis();
                        boolean sizeExceeded = lineBuffer.length() > maxChars;

                        if (hasNewline || timeoutReached || sizeExceeded) {
                            if (lineBuffer.length() > 0) {
                                sink.next(lineBuffer.toString());
                                lineBuffer.setLength(0);
                                lastEmit.set(now);
                            }
                        }
                    },
                    sink::error,
                    () -> {
                        // Emit remaining buffer on complete
                        if (lineBuffer.length() > 0) {
                            sink.next(lineBuffer.toString());
                        }
                        sink.complete();
                    });
        });
    }
}
