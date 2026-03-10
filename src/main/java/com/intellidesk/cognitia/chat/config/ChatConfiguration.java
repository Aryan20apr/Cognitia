package com.intellidesk.cognitia.chat.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.intellidesk.cognitia.analytics.utils.IdempotencyCallAdvisor;
import com.intellidesk.cognitia.analytics.utils.QuotaEnforcementAdvisor;
import com.intellidesk.cognitia.analytics.utils.TokenAnalyticsAdvisorV2;
import com.intellidesk.cognitia.chat.service.memory.SummarizingChatMemoryAdvisor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ChatConfiguration {

    // @Value("${title-generation.api-key}")
    // private String apiKey;

    // @Value("${spring.ai.openai.base-url}")
    // private String baseUrl;

    // @Value("${title-generation.model}")
    // private String titleModel;

    // @Value("${groq.api-key}")
    // private String groqApiKey;

    // @Value("${groq.base-url}")
    // private String groqBaseUrl;

    // @Value("${groq.chat.model}")
    // private String groqChatModel;

    @Value("${title-generation.api-key}")
    private String titleApiKey;

    @Value("${title-generation.base-url}")
    private String titleBaseUrl;

    @Value("${title-generation.model}")
    private String titleModel;

    @Bean
    public ChatMemory chatMemory(@Qualifier("redisChatMemoryRepository") ChatMemoryRepository repo,
                                 @Value("${cognitia.chat.memory.max-messages:50}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(maxMessages)
                .build();
    }

    @Bean
    public SummarizingChatMemoryAdvisor summarizingChatMemoryAdvisor(
            ChatMemory chatMemory,
            @Qualifier("lightClient") ChatClient lightClient,
            StringRedisTemplate stringRedisTemplate,
            @Value("${cognitia.chat.memory.recent-window-size:8}") int recentWindowSize,
            @Value("${cognitia.chat.memory.summarization-threshold:10}") int summarizationThreshold) {
        return SummarizingChatMemoryAdvisor.builder(chatMemory)
                .summaryClient(lightClient)
                .redisTemplate(stringRedisTemplate)
                .recentWindowSize(recentWindowSize)
                .summarizationThreshold(summarizationThreshold)
                .build();
    }

    // @Bean
    // public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
    //         VectorStore vectorStore,
    //         @Qualifier("lightClient") ChatClient lightClient) {
    //     return RetrievalAugmentationAdvisor.builder()
    //             .queryTransformers(RewriteQueryTransformer.builder()
    //                     .chatClientBuilder(lightClient.mutate())
    //                     .build())
    //             .documentRetriever(VectorStoreDocumentRetriever.builder()
    //                     .vectorStore(vectorStore)
    //                     .similarityThreshold(0.50)
    //                     .topK(3)
    //                     .build())
    //             .queryAugmenter(ContextualQueryAugmenter.builder()
    //                     .allowEmptyContext(true)
    //                     .build())
    //             .build();
    // }

    // @Bean
    // @Primary
    // public ChatModel groqChatModel() {
    //     OpenAiApi openAiApi = OpenAiApi.builder()
    //         .apiKey(groqApiKey)
    //         .baseUrl(groqBaseUrl)
    //         .build();

    //     OpenAiChatOptions options = OpenAiChatOptions.builder()
    //         .model(groqChatModel)
    //         .build();

    //     return OpenAiChatModel.builder()
    //         .openAiApi(openAiApi)
    //         .defaultOptions(options)
    //         .build();
    // }

    @Bean
    @Primary
    public ChatClient geminiChatClient(ChatModel chatModel, IdempotencyCallAdvisor idempotencyCallAdvisor, QuotaEnforcementAdvisor quotaEnforcementAdvisor, SummarizingChatMemoryAdvisor chatMemoryAdvisor, TokenAnalyticsAdvisorV2 tokenAnalyticsCallAdvisor) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(List.of(idempotencyCallAdvisor, quotaEnforcementAdvisor, chatMemoryAdvisor, tokenAnalyticsCallAdvisor, new SimpleLoggerAdvisor()))
            .build();
    }

    @Bean("lightClient")
    public ChatClient titleGenerationChatClient() {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(titleApiKey)
            .baseUrl(titleBaseUrl)
            .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(titleModel)
            .temperature(0.3)
            .maxTokens(300)
            .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(options)
            .build();

        return ChatClient.builder(chatModel)
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .build();
    }
}
