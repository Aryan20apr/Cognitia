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
import com.intellidesk.cognitia.chat.service.tools.DateTimeTool;
import com.intellidesk.cognitia.chat.service.tools.WebExtractTool;
import com.intellidesk.cognitia.chat.service.tools.WebSearchTool;

import lombok.RequiredArgsConstructor;



@Configuration
@RequiredArgsConstructor
public class ChatConfiguration {


    private final WebSearchTool webSearchTool;
    private final DateTimeTool dateTimeTool;
    private final WebExtractTool webExtractTool;

    @Value("${title-generation.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${title-generation.model}")
    private String titleModel;

    
    @Bean
    public ChatMemory chatMemory(@Qualifier("redisChatMemoryRepository") ChatMemoryRepository repo) {
        // MessageWindowChatMemory is the standard message window implementation
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repo)
                .maxMessages(20)
                .build();
    }
    
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    // @Bean
    // public QuestionAnswerAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
    //     return QuestionAnswerAdvisor.builder(vectorStore)
    //     .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(3).build())
    //     .build();
    // }
    
    @Bean
    @Primary
    public ChatClient geminiChatClient(ChatModel chatModel,  IdempotencyCallAdvisor idempotencyCallAdvisor, QuotaEnforcementAdvisor quotaEnforcementAdvisor, MessageChatMemoryAdvisor chatMemoryAdvisor, TokenAnalyticsAdvisorV2 tokenAnalyticsCallAdvisor) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(List.of(idempotencyCallAdvisor,quotaEnforcementAdvisor, chatMemoryAdvisor,tokenAnalyticsCallAdvisor, new SimpleLoggerAdvisor()))
            .defaultTools(webSearchTool, dateTimeTool, webExtractTool)
            .build();
        // return ChatClient.builder(chatModel).defaultAdvisors(List.of(new SimpleLoggerAdvisor())).build();
    }

    @Bean("lightClient")
    public ChatClient titleGenerationChatClient() {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(titleModel)
            .temperature(0.3)  // Lower temperature for consistent response like title generation cases
            .maxTokens(50)     // Titles are short
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
