package com.intellidesk.cognitia.chat.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.intellidesk.cognitia.chat.models.entities.StoredMessage;
import com.intellidesk.cognitia.chat.repository.RedisChatMemoryRepository;
import com.intellidesk.cognitia.chat.service.RetrievalAugmentationAdvisor;


@Configuration
public class ChatConfiguration {

    // @Bean
    // public ChatMemoryRepository chatMemoryRepository(RedisTemplate<String, StoredMessage> redisTemplate) {
    //     return new RedisChatMemoryRepository(redisTemplate, "cognitia:chat:memory:");
    // }
    
    // @Bean
    // public ChatMemory chatMemory(ChatMemoryRepository repo) {
    //     // MessageWindowChatMemory is the standard message window implementation
    //     return MessageWindowChatMemory.builder()
    //             .chatMemoryRepository(repo)
    //             .maxMessages(20)
    //             .build();
    // }
    
    // @Bean
    // public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
    //     return MessageChatMemoryAdvisor.builder(chatMemory).build();
    // }

    // @Bean
    // public QuestionAnswerAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
    //     return QuestionAnswerAdvisor.builder(vectorStore)
    //     .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(3).build())
    //     .build();
    // }
    
    @Bean
    public ChatClient geminiChatClient(ChatModel chatModel) {
        // return ChatClient.builder(chatModel).defaultAdvisors(List.of(memoryAdvisor, ragAdvisor,new SimpleLoggerAdvisor())).build();
        return ChatClient.builder(chatModel).defaultAdvisors(List.of(new SimpleLoggerAdvisor())).build();
    }
}
