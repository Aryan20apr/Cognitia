package com.intellidesk.cognitia.analytics.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.analytics.models.dto.ChatUsageDetailsDTO;
import com.intellidesk.cognitia.analytics.repository.ChatUsageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageAggregatorService {


    private final ChatUsageRepository eventRepository;
    private final QuotaService quotaService;
    private final RedisIdempotencyService redisIdempotencyService;
    // Optionally, inject billingService if immediate invoice generation is needed

    /**
     * Process incoming usage events (from MQ, HTTP, or batch job).
     */
    @Transactional
    @KafkaListener(topics = "${analytics.usage-events.topic.name}", groupId = "${analytics.usage-events.group.name}", containerFactory = "usageEventsKafkaListenerContainerFactory")
    public void processUsageEvent(ChatUsageDetailsDTO event) {
        try {
            log.info("Received usage event: {}", event);

            boolean exists = eventRepository.existsByRequestId(event.getRequestId());
            log.info("Checked existence of event with requestId={}: {}", event.getRequestId(), exists);

            if (!exists) {
                log.warn("Event with requestId={} not found in DB. It might be unpersisted or delayed.", event.getRequestId());
                // You could optionally decide to persist or skip it here
            } else {
                log.info("Event with requestId={} found in DB, proceeding with aggregation.", event.getRequestId());
            }

            log.info("Recording usage to QuotaService for tenantId={}, userId={}, threadId={}", event.getTenantId(), event.getUserId(), event.getThreadId());
            quotaService.recordUsage(event);

            // Mark processed in redis for duplicate protection
            if (event.getRequestId() != null) {
                log.info("Marking requestId={} as processed in Redis", event.getRequestId());
                redisIdempotencyService.markProcessed(event.getRequestId());
            }

            // Optionally push to Billing service for immediate or deferred invoice update
            // billingService.updateInvoiceAsync(event);

            log.info("Successfully recorded usage for tenant={} requestId={}", event.getTenantId(), event.getRequestId());

        } catch (Exception e) {
            log.error("Failed to process usage event: {}", event, e);
            // Optionally push to DLQ or retry queue
        }
    }
}
