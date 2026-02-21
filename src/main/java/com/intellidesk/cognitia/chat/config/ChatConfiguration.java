package com.intellidesk.cognitia.chat.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.intellidesk.cognitia.analytics.utils.IdempotencyCallAdvisor;
import com.intellidesk.cognitia.analytics.utils.QuotaEnforcementAdvisor;
import com.intellidesk.cognitia.analytics.utils.TokenAnalyticsAdvisorV2;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ChatConfiguration {

    @Value("${title-generation.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${title-generation.model}")
    private String titleModel;

    @Bean
    public ChatMemory chatMemory(@Qualifier("redisChatMemoryRepository") ChatMemoryRepository repo) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(20)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    @Primary
    public ChatClient geminiChatClient(ChatModel chatModel, IdempotencyCallAdvisor idempotencyCallAdvisor, QuotaEnforcementAdvisor quotaEnforcementAdvisor, MessageChatMemoryAdvisor chatMemoryAdvisor, TokenAnalyticsAdvisorV2 tokenAnalyticsCallAdvisor) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(List.of(idempotencyCallAdvisor, quotaEnforcementAdvisor, chatMemoryAdvisor, tokenAnalyticsCallAdvisor, new SimpleLoggerAdvisor()))
            .build();
    }

    @Bean("lightClient")
    public ChatClient titleGenerationChatClient() {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
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
