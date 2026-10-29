package com.intellidesk.cognitia.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.chat.models.entities.ChatShare;

public interface ChatShareRepository extends JpaRepository<ChatShare, UUID> {

    Optional<ChatShare> findByToken(String token);

    List<ChatShare> findAllByOwner_Id(UUID ownerId);

    Optional<ChatShare> findBySourceThread_IdAndOwner_Id(UUID threadId, UUID ownerId);
}
