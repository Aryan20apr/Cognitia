package com.intellidesk.cognitia.ingestion.storage.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.ingestion.storage.models.entities.IngestionOutbox;
import com.intellidesk.cognitia.ingestion.storage.models.entities.RawSouce;
import com.intellidesk.cognitia.ingestion.storage.models.enums.IngestionStatus;
import com.intellidesk.cognitia.ingestion.storage.repository.ResourceOutboxRepository;
import com.intellidesk.cognitia.ingestion.storage.service.IngetionSchedularService;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class IngestionSchedularServiceImpl implements IngetionSchedularService {

    private final ResourceOutboxRepository resourceOutboxRepository;
    private final KafkaTemplate<String, RawSouce> kafkaTemplate;

    @Value("${ingestion.topic.name}")
    private String topic;

    private final ObjectMapper objectMapper;

    @Override
    @Scheduled(fixedRate = 60000)
    public void processPendingResourceIngestions() {
        log.info("Starting to process pending resource ingestions");

        List<IngestionOutbox> pendingIngestions = resourceOutboxRepository
                .findByStatusOrderByCreatedAtAsc(IngestionStatus.PENDING_PROCESSING);

        if (pendingIngestions.isEmpty()) {
            log.info("No pending ingestions found");
            return;
        }

        pendingIngestions.parallelStream().forEach(ingestion -> {
            try {
                RawSouce rawSource = ingestion.getSource(); // Get associated RawSource

                if (rawSource == null) {
                    log.error("No RawSource found for ingestion ID: {}", ingestion.getId());
                    updateIngestionStatus(ingestion, IngestionStatus.FAILED);
                    return;
                }

                // Publish to Kafka topic
                kafkaTemplate.send(topic, rawSource.getResId().toString(), rawSource)
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
