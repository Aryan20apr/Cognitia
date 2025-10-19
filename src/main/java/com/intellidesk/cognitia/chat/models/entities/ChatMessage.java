package com.intellidesk.cognitia.chat.models.entities;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private ChatThread thread;

    private MessageType sender;  // "USER" or "AI"
    @Column(columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false )
    private Date createdAt;
}