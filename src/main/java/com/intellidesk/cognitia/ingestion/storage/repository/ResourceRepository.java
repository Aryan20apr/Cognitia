package com.intellidesk.cognitia.ingestion.storage.repository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.intellidesk.cognitia.ingestion.storage.models.entities.RawSouce;

public interface ResourceRepository extends JpaRepository<RawSouce, UUID> {}