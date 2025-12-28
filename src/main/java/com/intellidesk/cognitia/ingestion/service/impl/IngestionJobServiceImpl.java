package com.intellidesk.cognitia.ingestion.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.ingestion.models.dtos.IngestionJobDetails;
import com.intellidesk.cognitia.ingestion.repository.IngestionJobRepository;
import com.intellidesk.cognitia.ingestion.service.IngestionJobService;
@Service
public class IngestionJobServiceImpl implements IngestionJobService {
    
    private final IngestionJobRepository ingestionJobRepository;

    public IngestionJobServiceImpl(IngestionJobRepository ingestionJobRepository) {
        this.ingestionJobRepository = ingestionJobRepository;
    }

    @Override
    @Transactional
    public Page<IngestionJobDetails> getIngestionJobDetails(int page, int size) {
        return ingestionJobRepository.findOutboxPage(PageRequest.of(page, size));
    }
}
