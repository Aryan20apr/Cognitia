package com.intellidesk.cognitia.chat.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.chat.models.entities.ChatShareMessage;

public interface ChatShareMessageRepository extends JpaRepository<ChatShareMessage, UUID> {

    List<ChatShareMessage> findAllByShare_IdAndSnapshotVersionOrderBySequenceNoAsc(UUID shareId, Integer snapshotVersion);
}
