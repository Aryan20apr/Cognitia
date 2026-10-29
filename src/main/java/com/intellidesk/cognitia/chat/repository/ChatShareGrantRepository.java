package com.intellidesk.cognitia.chat.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.intellidesk.cognitia.chat.models.entities.ChatShareGrant;

public interface ChatShareGrantRepository extends JpaRepository<ChatShareGrant, UUID> {

    boolean existsByShare_IdAndGranteeUser_Id(UUID shareId, UUID userId);

    boolean existsByShare_IdAndGranteeRole_RoleId(UUID shareId, Integer roleId);

    List<ChatShareGrant> findAllByShare_Id(UUID shareId);

    List<ChatShareGrant> findAllByGranteeUser_Id(UUID userId);

    List<ChatShareGrant> findAllByGranteeRole_RoleId(Integer roleId);
}
