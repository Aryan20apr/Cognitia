package com.intellidesk.cognitia.ingestion.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.ingestion.models.entities.IngestionOutbox;
import com.intellidesk.cognitia.ingestion.models.entities.RawSouce;
import com.intellidesk.cognitia.ingestion.models.enums.IngestionStatus;
import com.intellidesk.cognitia.ingestion.repository.ResourceOutboxRepository;
import com.intellidesk.cognitia.ingestion.service.IngetionSchedularService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class IngestionSchedularServiceImpl implements IngetionSchedularService {

    private final ResourceOutboxRepository resourceOutboxRepository;
    private final KafkaTemplate<String, IngestionOutbox> kafkaTemplate;

    @Value("${ingestion.topic.name}")
    private String topic;

    private final ObjectMapper objectMapper;

    @Override
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processPendingResourceIngestions() {
        log.info("Starting to process pending resource ingestions");

        List<IngestionOutbox> pendingIngestions = resourceOutboxRepository
                .findByStatusOrderByCreatedAtAsc(IngestionStatus.FAILED);

        if (pendingIngestions.isEmpty()) {
            log.info("No pending ingestions found");
            return;
        }

        pendingIngestions.forEach(ingestion -> {
            try {
               

              

                // Publish to Kafka topic
                kafkaTemplate.send(topic, ingestion.getId().toString(), ingestion)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish message for ingestion ID: {}", ingestion.getId(), ex);
                                handleFailedIngestion(ingestion);
                            } else {
                                log.info("Successfully published message for ingestion ID: {}", ingestion.getId());
                                updateIngestionStatus(ingestion, IngestionStatus.PUBLISHED);
                            }
                        });

            } catch (Exception e) {
                log.error("Error processing ingestion ID: {}", ingestion.getId(), e);
                handleFailedIngestion(ingestion);
            }
        });
    }

    private void handleFailedIngestion(IngestionOutbox ingestion) {
        ingestion.setRetries(ingestion.getRetries() + 1);
        updateIngestionStatus(ingestion, IngestionStatus.FAILED);
    }

    private void updateIngestionStatus(IngestionOutbox ingestion, IngestionStatus status) {
        ingestion.setStatus(status);
        resourceOutboxRepository.save(ingestion);
        log.info("Updated ingestion ID: {} status to: {}", ingestion.getId(), status);
    }

}
