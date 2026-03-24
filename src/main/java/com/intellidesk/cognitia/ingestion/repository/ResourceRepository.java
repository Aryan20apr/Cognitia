package com.intellidesk.cognitia.ingestion.repository;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.ingestion.models.entities.Resource;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Optional<Resource> findByAssetId(String assetId);
}