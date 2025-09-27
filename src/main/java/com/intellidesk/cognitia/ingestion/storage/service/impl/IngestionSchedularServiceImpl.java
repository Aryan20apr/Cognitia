package com.intellidesk.cognitia.ingestion.storage.service.impl;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;

import com.intellidesk.cognitia.ingestion.storage.models.entities.IngestionOutbox;
import com.intellidesk.cognitia.ingestion.storage.models.enums.IngestionStatus;
import com.intellidesk.cognitia.ingestion.storage.repository.ResourceOutboxRepository;
import com.intellidesk.cognitia.ingestion.storage.service.IngetionSchedularService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class IngestionSchedularServiceImpl implements IngetionSchedularService {

    private ResourceOutboxRepository resourceOutboxRepository;

    @Override
    @Scheduled(fixedRate = 60000)
    public void processPendingResourceIngestions() {
        // Implementation for processing pending resource ingestions

        List<IngestionOutbox> pendingIngestions = resourceOutboxRepository.findByStatusOrderByCreatedAtAsc(IngestionStatus.PENDING_PROCESSING);


    }
    
}
