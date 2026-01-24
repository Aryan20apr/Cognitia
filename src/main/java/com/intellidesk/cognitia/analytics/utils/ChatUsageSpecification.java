package com.intellidesk.cognitia.analytics.utils;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

import com.intellidesk.cognitia.analytics.models.entity.ChatUsage;


public class ChatUsageSpecification {

    /** Filter by User ID */
    public static Specification<ChatUsage> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? null 
                : cb.equal(root.get("user").get("id"), userId);
    }

    /** Filter by Thread ID */
    public static Specification<ChatUsage> hasThreadId(UUID threadId) {
        return (root, query, cb) -> threadId == null ? null 
                : cb.equal(root.get("thread").get("id"), threadId);
    }
}
