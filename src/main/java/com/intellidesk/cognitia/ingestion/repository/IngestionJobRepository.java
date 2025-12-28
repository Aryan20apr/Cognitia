package com.intellidesk.cognitia.ingestion.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.intellidesk.cognitia.ingestion.models.dtos.IngestionJobDetails;
import com.intellidesk.cognitia.ingestion.models.entities.IngestionJob;
import com.intellidesk.cognitia.ingestion.models.enums.IngestionStatus;


public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {

      List<IngestionJob> findByStatusOrderByCreatedAtAsc(IngestionStatus status);


      @Query("""
        select new com.intellidesk.cognitia.ingestion.models.dtos.IngestionJobDetails(
            o.id,
            o.status,
            o.retries,
            r.resId,
            r.name,
            r.status,
            o.createdAt
        )
        from IngestionJob o
        join o.source r
        order by o.createdAt desc
    """)
      Page<IngestionJobDetails> findOutboxPage(Pageable pageable);


}
