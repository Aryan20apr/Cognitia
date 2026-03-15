package com.intellidesk.cognitia.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.intellidesk.cognitia.chat.models.entities.ChatThread;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatThread t SET t.title = :title WHERE t.id = :id")
    int updateTitleById(@Param("id") UUID id, @Param("title") String title);

    List<ChatThread> findAllByUserId(UUID userId);

    Optional<ChatThread> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatThread t SET t.title = :title WHERE t.id = :id AND t.user.id = :userId")
    int updateTitleByIdAndUserId(@Param("id") UUID id, @Param("title") String title, @Param("userId") UUID userId);
}