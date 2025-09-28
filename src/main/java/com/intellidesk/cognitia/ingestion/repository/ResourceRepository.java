package com.intellidesk.cognitia.ingestion.repository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.intellidesk.cognitia.ingestion.models.entities.RawSouce;

public interface ResourceRepository extends JpaRepository<RawSouce, UUID> {}