package com.intellidesk.cognitia.ingestion.config;

import org.springframework.ai.document.DocumentWriter;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.writer.FileDocumentWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableJpaAuditing
public class IngestionConfig {
    

    @Bean
    public TextSplitter tokenTextSplitter(){
        return new TokenTextSplitter(1000, 400, 10, 5000, true);
    }

    @Bean
    public DocumentWriter documentWriter(){
        return new FileDocumentWriter("output.json",true,MetadataMode.ALL, false);
    }
}
