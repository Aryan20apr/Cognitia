package com.intellidesk.cognitia.ingestion.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.ingestion.models.entities.IngestionOutbox;
import com.intellidesk.cognitia.ingestion.models.enums.IngestionStatus;


public interface ResourceOutboxRepository extends JpaRepository<IngestionOutbox, UUID> {
      List<IngestionOutbox> findByStatusOrderByCreatedAtAsc(IngestionStatus status);
}
