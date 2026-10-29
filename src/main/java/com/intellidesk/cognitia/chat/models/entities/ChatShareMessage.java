package com.intellidesk.cognitia.chat.models.entities;

import java.time.Instant;
import java.util.UUID;

import org.springframework.ai.chat.messages.MessageType;

import com.intellidesk.cognitia.utils.uuidv7.GeneratedUuidV7;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "chat_share_messages")
public class ChatShareMessage {

    @Id
    @GeneratedUuidV7
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id", nullable = false)
    private ChatShare share;

    @Column(name = "snapshot_version", nullable = false)
    private Integer snapshotVersion;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "sender", nullable = false)
    private MessageType sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "original_message_id")
    private UUID originalMessageId;

    @Column(name = "original_created_at", nullable = false)
    private Instant originalCreatedAt;
}
