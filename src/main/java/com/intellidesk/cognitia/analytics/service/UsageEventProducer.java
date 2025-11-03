package com.intellidesk.cognitia.analytics.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;

@Service
public class UsageEventProducer {

    private final KafkaTemplate<String, ChatUsageDetailsDTO> kafkaTemplate;
    private final String topic = "cognitia-chat-usage-events";

    public UsageEventProducer(KafkaTemplate<String, ChatUsageDetailsDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ChatUsageDetailsDTO dto) {
        kafkaTemplate.send(topic, dto.getRequestId(), dto);
    }
}