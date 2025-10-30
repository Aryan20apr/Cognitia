package com.intellidesk.cognitia.chat.models.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;
import org.springframework.data.annotation.CreatedDate;

import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;
import com.intellidesk.cognitia.userandauth.models.entities.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "chat_threads")
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class ChatThread extends TenantAwareEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @CreatedDate
    @CreationTimestamp
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();


    public void addMessage(ChatMessage chatMessage){
        messages.add(chatMessage);
    }
}
