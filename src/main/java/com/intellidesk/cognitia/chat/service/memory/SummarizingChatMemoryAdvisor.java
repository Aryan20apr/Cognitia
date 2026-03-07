package com.intellidesk.cognitia.chat.service.memory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
public final class SummarizingChatMemoryAdvisor implements BaseChatMemoryAdvisor {

    private static final String SUMMARY_CACHE_PREFIX = "cognitia:chat:summary:";
    private static final Duration SUMMARY_TTL = Duration.ofHours(6);

    private final ChatMemory chatMemory;
    private final ChatClient summaryClient;
    private final StringRedisTemplate redisTemplate;
    private final String defaultConversationId;
    private final int order;
    private final Scheduler scheduler;
    private final int recentWindowSize;
    private final int summarizationThreshold;

    private SummarizingChatMemoryAdvisor(ChatMemory chatMemory,
                                         ChatClient summaryClient,
                                         StringRedisTemplate redisTemplate,
                                         String defaultConversationId,
                                         int order,
                                         Scheduler scheduler,
                                         int recentWindowSize,
                                         int summarizationThreshold) {
        this.chatMemory = chatMemory;
        this.summaryClient = summaryClient;
        this.redisTemplate = redisTemplate;
        this.defaultConversationId = defaultConversationId;
        this.order = order;
        this.scheduler = scheduler;
        this.recentWindowSize = recentWindowSize;
        this.summarizationThreshold = summarizationThreshold;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String conversationId = getConversationId(chatClientRequest.context(), this.defaultConversationId);

        List<Message> allMessages = this.chatMemory.get(conversationId);

        List<Message> processedMessages;
        if (allMessages.size() > summarizationThreshold) {
            processedMessages = buildSummarizedMessages(conversationId, allMessages);
        } else {
            processedMessages = new ArrayList<>(allMessages);
        }

        processedMessages.addAll(chatClientRequest.prompt().getInstructions());

        for (int i = 0; i < processedMessages.size(); i++) {
            if (processedMessages.get(i) instanceof SystemMessage) {
                Message systemMessage = processedMessages.remove(i);
                processedMessages.add(0, systemMessage);
                break;
            }
        }

        ChatClientRequest processedRequest = chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
                .build();

        Message userMessage = processedRequest.prompt().getLastUserOrToolResponseMessage();
        this.chatMemory.add(conversationId, userMessage);

        return processedRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        List<Message> assistantMessages = new ArrayList<>();
        if (chatClientResponse.chatResponse() != null) {
            assistantMessages = chatClientResponse.chatResponse()
                    .getResults()
                    .stream()
                    .map(g -> (Message) g.getOutput())
                    .toList();
        }
        this.chatMemory.add(
                this.getConversationId(chatClientResponse.context(), this.defaultConversationId),
                assistantMessages);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                  StreamAdvisorChain streamAdvisorChain) {
        Scheduler sch = this.getScheduler();

        return Mono.just(chatClientRequest)
                .publishOn(sch)
                .map(request -> this.before(request, streamAdvisorChain))
                .flatMapMany(streamAdvisorChain::nextStream)
                .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
                        response -> this.after(response, streamAdvisorChain)));
    }

    private List<Message> buildSummarizedMessages(String conversationId, List<Message> allMessages) {
        int splitPoint = allMessages.size() - recentWindowSize;
        List<Message> olderMessages = allMessages.subList(0, splitPoint);
        List<Message> recentMessages = allMessages.subList(splitPoint, allMessages.size());

        String summary = getOrCreateSummary(conversationId, olderMessages);

        List<Message> result = new ArrayList<>();
        result.add(new SystemMessage("Summary of earlier conversation:\n" + summary));
        result.addAll(recentMessages);
        return result;
    }

    private String getOrCreateSummary(String conversationId, List<Message> messagesToSummarize) {
        String contentHash = computeHash(messagesToSummarize);
        String cacheKey = SUMMARY_CACHE_PREFIX + conversationId + ":" + contentHash;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("[SummarizingMemory] Cache hit for conversation {}", conversationId);
            return cached;
        }

        log.info("[SummarizingMemory] Generating summary for {} messages in conversation {}",
                messagesToSummarize.size(), conversationId);

        String transcript = messagesToSummarize.stream()
                .map(m -> m.getMessageType().name() + ": " + m.getText())
                .collect(Collectors.joining("\n"));

        String summary;
        try {
            summary = summaryClient.prompt()
                    .system("""
                            You are a conversation summarizer. Produce a concise 2-4 sentence summary \
                            of the following conversation transcript. Preserve key facts, decisions, \
                            names, numbers, and any commitments made. Do NOT add any preamble or \
                            explanation — output ONLY the summary text.""")
                    .user(transcript)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("[SummarizingMemory] Summary generation failed, falling back to truncation: {}",
                    e.getMessage());
            summary = truncateFallback(messagesToSummarize);
        }

        if (summary != null && !summary.isBlank()) {
            redisTemplate.opsForValue().set(cacheKey, summary, SUMMARY_TTL);
        }

        return summary != null ? summary : "";
    }

    private String truncateFallback(List<Message> messages) {
        int take = Math.min(messages.size(), 4);
        return messages.subList(messages.size() - take, messages.size()).stream()
                .map(m -> m.getMessageType().name() + ": " + m.getText())
                .collect(Collectors.joining("\n"));
    }

    private String computeHash(List<Message> messages) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Message m : messages) {
                md.update(m.getMessageType().name().getBytes(StandardCharsets.UTF_8));
                md.update((byte) ':');
                String text = m.getText() != null ? m.getText() : "";
                md.update(text.getBytes(StandardCharsets.UTF_8));
                md.update((byte) '\n');
            }
            byte[] digest = md.digest();
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static Builder builder(ChatMemory chatMemory) {
        return new Builder(chatMemory);
    }

    public static final class Builder {

        private final ChatMemory chatMemory;
        private ChatClient summaryClient;
        private StringRedisTemplate redisTemplate;
        private String conversationId = ChatMemory.DEFAULT_CONVERSATION_ID;
        private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;
        private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;
        private int recentWindowSize = 8;
        private int summarizationThreshold = 10;

        private Builder(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
        }

        public Builder summaryClient(ChatClient summaryClient) {
            this.summaryClient = summaryClient;
            return this;
        }

        public Builder redisTemplate(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder recentWindowSize(int recentWindowSize) {
            this.recentWindowSize = recentWindowSize;
            return this;
        }

        public Builder summarizationThreshold(int summarizationThreshold) {
            this.summarizationThreshold = summarizationThreshold;
            return this;
        }

        public SummarizingChatMemoryAdvisor build() {
            if (this.summaryClient == null) {
                throw new IllegalArgumentException("summaryClient is required");
            }
            if (this.redisTemplate == null) {
                throw new IllegalArgumentException("redisTemplate is required");
            }
            return new SummarizingChatMemoryAdvisor(
                    this.chatMemory,
                    this.summaryClient,
                    this.redisTemplate,
                    this.conversationId,
                    this.order,
                    this.scheduler,
                    this.recentWindowSize,
                    this.summarizationThreshold);
        }
    }
}
