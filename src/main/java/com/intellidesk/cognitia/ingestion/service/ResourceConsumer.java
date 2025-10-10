package com.intellidesk.cognitia.ingestion.service;

import java.util.concurrent.CountDownLatch;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.ingestion.models.entities.IngestionOutbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ResourceConsumer {
    
    private final PreprocessingService preprocessingService;

     public CountDownLatch latch = new CountDownLatch(3);

    @KafkaListener(topics = "${ingestion.topic.name}", groupId = "${ingestion.group.name}", containerFactory = "ingestionKafkaListenerContainerFactory")
    public void listenGroup(IngestionOutbox message) {
        
        log.info("Received Message in group ingestion-group: " + message);
        preprocessingService.preprocessFile(message);
        latch.countDown();
    }
}

