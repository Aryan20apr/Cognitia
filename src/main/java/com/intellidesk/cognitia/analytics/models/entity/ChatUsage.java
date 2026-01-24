package com.intellidesk.cognitia.analytics.models.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Filters;

import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.userandauth.models.entities.TenantAwareEntity;
import com.intellidesk.cognitia.userandauth.models.entities.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_usage_event", indexes = {
        @Index(name = "idx_cue_tenant", columnList = "tenant_id"),
        @Index(name = "idx_cue_user", columnList = "user_id"),
        @Index(name = "idx_cue_request", columnList = "request_id")
})
@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class ChatUsage extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name= "request_id", unique=true, nullable=false)
    private String requestId;

    @ManyToOne
    @JoinColumn(name="user_id", referencedColumnName = "id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "thread_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ChatThread thread;

    private Long promptTokens;      
    private Long completionTokens;  
    private Long totalTokens;       // total = prompt + completion

    private Boolean isProcessed;
    
    private Double estimatedCost;

    
    private String modelName;

    @Column(columnDefinition = "TEXT")
    private String metaDataJson;

    
    private Long latencyMs; // time taken for model response

    private Date processedAt;

    @CreationTimestamp
    @CreatedDate
    private Date createdAt;

    @UpdateTimestamp
    @LastModifiedDate
    private Date updatedAt;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((requestId == null) ? 0 : requestId.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((thread == null) ? 0 : thread.hashCode());
        result = prime * result + ((promptTokens == null) ? 0 : promptTokens.hashCode());
        result = prime * result + ((completionTokens == null) ? 0 : completionTokens.hashCode());
        result = prime * result + ((totalTokens == null) ? 0 : totalTokens.hashCode());
        result = prime * result + ((isProcessed == null) ? 0 : isProcessed.hashCode());
        result = prime * result + ((estimatedCost == null) ? 0 : estimatedCost.hashCode());
        result = prime * result + ((modelName == null) ? 0 : modelName.hashCode());
        result = prime * result + ((latencyMs == null) ? 0 : latencyMs.hashCode());
        result = prime * result + ((processedAt == null) ? 0 : processedAt.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ChatUsage other = (ChatUsage) obj;
        if (requestId == null) {
            if (other.requestId != null)
                return false;
        } else if (!requestId.equals(other.requestId))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (thread == null) {
            if (other.thread != null)
                return false;
        } else if (!thread.equals(other.thread))
            return false;
        if (promptTokens == null) {
            if (other.promptTokens != null)
                return false;
        } else if (!promptTokens.equals(other.promptTokens))
            return false;
        if (completionTokens == null) {
            if (other.completionTokens != null)
                return false;
        } else if (!completionTokens.equals(other.completionTokens))
            return false;
        if (totalTokens == null) {
            if (other.totalTokens != null)
                return false;
        } else if (!totalTokens.equals(other.totalTokens))
            return false;
        if (isProcessed == null) {
            if (other.isProcessed != null)
                return false;
        } else if (!isProcessed.equals(other.isProcessed))
            return false;
        if (estimatedCost == null) {
            if (other.estimatedCost != null)
                return false;
        } else if (!estimatedCost.equals(other.estimatedCost))
            return false;
        if (modelName == null) {
            if (other.modelName != null)
                return false;
        } else if (!modelName.equals(other.modelName))
            return false;
        if (latencyMs == null) {
            if (other.latencyMs != null)
                return false;
        } else if (!latencyMs.equals(other.latencyMs))
            return false;
        if (processedAt == null) {
            if (other.processedAt != null)
                return false;
        } else if (!processedAt.equals(other.processedAt))
            return false;
        return true;
    }

    

}