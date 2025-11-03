package com.intellidesk.cognitia.analytics.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.repository.ChatUsageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageAggregatorService {

    private final ObjectMapper objectMapper;
    private final ChatUsageRepository eventRepository;
    private final QuotaService quotaService;
    // Optionally, inject billingService if immediate invoice generation is needed

    /**
     * Process incoming usage events (from MQ, HTTP, or batch job).
     */
    @Transactional
    @KafkaListener(topics = "${analytics.usage-events.topic.name}", groupId = "${analytics.usage-events.group.name}", containerFactory = "ingestionKafkaListenerContainerFactory")
    public void processUsageEvent(ChatUsageDetailsDTO event) {
        try {
            
            log.debug("Received usage event: {}", event);

            boolean exists = eventRepository.existsByRequestId(event.getRequestId());
            if (!exists) {
                log.warn("Event with requestId={} not found in DB. It might be unpersisted or delayed.", event.getRequestId());
                // You could optionally decide to persist or skip it here
            }

            //    Record usage to Quota Service (idempotent, Redis-backed)
            //    Note: QuotaService.recordUsage internally handles concurrency and deduplication
            quotaService.recordUsage(
               event
            );

            // Optionally push to Billing service for immediate or deferred invoice update
            // billingService.updateInvoiceAsync(event);

            log.info("Successfully recorded usage for tenant={} requestId={}", event.getTenantId(), event.getRequestId());

        } catch (Exception e) {
            log.error("Failed to process usage event: {}", event, e);
            // Optionally push to DLQ or retry queue
        }
    }
}
