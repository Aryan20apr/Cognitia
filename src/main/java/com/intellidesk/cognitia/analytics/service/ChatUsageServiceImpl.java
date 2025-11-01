package com.intellidesk.cognitia.analytics.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.models.entity.ChatUsage;
import com.intellidesk.cognitia.analytics.repository.ChatUsageRepository;
import com.intellidesk.cognitia.analytics.utils.ChatUsageMapper;
import com.intellidesk.cognitia.analytics.utils.ChatUsageSpecification;
import com.intellidesk.cognitia.chat.models.entities.ChatThread;
import com.intellidesk.cognitia.userandauth.models.entities.Tenant;
import com.intellidesk.cognitia.userandauth.models.entities.User;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ChatUsageServiceImpl implements ChatUsageService {


    private final ChatUsageMapper chatUsageMapper; // MapStruct bean
    private final ChatUsageRepository chatUsageRepository;


    @Override
    @Transactional
    public ChatUsageDetailsDTO saveChatUsage(ChatUsageDetailsDTO chatUsageDetailsDTO) {
        ChatUsage chatUsage = chatUsageMapper.toEntity(chatUsageDetailsDTO);
        User user = new User();
        user.setId(chatUsageDetailsDTO.getUserId());
        chatUsage.setUser(user);

        Tenant tenant = new Tenant();
        tenant.setId(chatUsageDetailsDTO.getTenantId());
        chatUsage.setTenant(tenant);

        ChatThread thread = new ChatThread();
        thread.setId(chatUsageDetailsDTO.getThreadId());
        chatUsage.setThread(thread);


        ChatUsage saved = chatUsageRepository.save(chatUsage);
        return chatUsageMapper.toDTO(saved);
    }

    @Override
public List<ChatUsageDetailsDTO> getChatUsageData(String userId, String tenantId, String threadId) {
    
    UUID userUUID = null;
    UUID tenantUUID = null;
    UUID threadUUID = null;

    try {
        if (userId != null) userUUID = UUID.fromString(userId);
        if (tenantId != null) tenantUUID = UUID.fromString(tenantId);
        if (threadId != null) threadUUID = UUID.fromString(threadId);
    } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("Invalid UUID format", ex);
    }

    // 2️⃣ Build dynamic JPA Specification
    Specification<ChatUsage> spec = Specification
            .where(userUUID != null ? ChatUsageSpecification.hasUserId(userUUID) : null)
            .and(tenantUUID != null ? ChatUsageSpecification.hasTenantId(tenantUUID) : null)
            .and(threadUUID != null ? ChatUsageSpecification.hasThreadId(threadUUID) : null);

    // 3️⃣ Fetch data from repository
    List<ChatUsage> chatUsages = chatUsageRepository.findAll(spec);

    // 4️⃣ Map entities to DTOs using MapStruct
    return chatUsages.stream()
                     .map(chatUsageMapper::toDTO)
                     .collect(Collectors.toList());
}

    
}
