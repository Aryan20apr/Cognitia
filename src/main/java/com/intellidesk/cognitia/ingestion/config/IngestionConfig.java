package com.intellidesk.cognitia.ingestion.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.writer.FileDocumentWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.intellidesk.cognitia.ingestion.service.transformer.ContextualChunkEnricher;
import com.intellidesk.cognitia.ingestion.service.transformer.DocumentContextExtractor;

@Configuration
@EnableScheduling
@EnableJpaAuditing
public class IngestionConfig {

    @Bean
    public TextSplitter tokenTextSplitter(){
        return TokenTextSplitter.builder()
        .withChunkSize(800)
        .withMinChunkSizeChars(300)
        .withMinChunkLengthToEmbed(10)
        .withMaxNumChunks(5000)
        .withKeepSeparator(true)
        .build();
    }

    @Bean
    public DocumentWriter documentWriter(){
        return new FileDocumentWriter("output.json",true,MetadataMode.ALL, false);
    }

    @Bean
    public DocumentContextExtractor documentContextExtractor(
            @Qualifier("lightClient") ChatClient lightClient,
            @Value("${ingestion.contextual-enrichment.context-chunks:5}") int contextChunks) {
        return new DocumentContextExtractor(lightClient, contextChunks);
    }

    @Bean
    public ContextualChunkEnricher contextualChunkEnricher(
            @Qualifier("lightClient") ChatClient lightClient) {
        return new ContextualChunkEnricher(lightClient);
    }
}
