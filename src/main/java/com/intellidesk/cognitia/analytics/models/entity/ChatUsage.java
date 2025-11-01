package com.intellidesk.cognitia.analytics.models.entity;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.userandauth.models.entities.Tenant;
import com.intellidesk.cognitia.userandauth.models.entities.User;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_usage")
@Builder
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChatUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name="user_id", referencedColumnName = "id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "teant_id", referencedColumnName = "id")
    private Tenant tenant;

    @ManyToOne
    @JoinColumn(name = "thread_id", referencedColumnName = "id")
    private ChatThread thread;

    private Long promptTokens;      
    private Long completionTokens;  
    private Long totalTokens;       // total = prompt + completion

    
    private Double estimatedCost;

    
    private String modelName;

    
    private Long latencyMs; // time taken for model response

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
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
        result = prime * result + ((thread == null) ? 0 : thread.hashCode());
        result = prime * result + ((promptTokens == null) ? 0 : promptTokens.hashCode());
        result = prime * result + ((completionTokens == null) ? 0 : completionTokens.hashCode());
        result = prime * result + ((totalTokens == null) ? 0 : totalTokens.hashCode());
        result = prime * result + ((estimatedCost == null) ? 0 : estimatedCost.hashCode());
        result = prime * result + ((modelName == null) ? 0 : modelName.hashCode());
        result = prime * result + ((latencyMs == null) ? 0 : latencyMs.hashCode());
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
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (tenant == null) {
            if (other.tenant != null)
                return false;
        } else if (!tenant.equals(other.tenant))
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
        return true;
    }



    
}

