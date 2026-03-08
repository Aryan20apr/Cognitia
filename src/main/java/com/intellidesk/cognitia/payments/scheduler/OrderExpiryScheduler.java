package com.intellidesk.cognitia.payments.scheduler;

import java.time.OffsetDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.payments.models.enums.OrderStatus;
import com.intellidesk.cognitia.payments.repository.OrderRepository;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryScheduler {

    private final OrderRepository orderRepository;

    private static final int STALE_THRESHOLD_MINUTES = 30;

    @Scheduled(fixedDelay = 300_000)
    @SchedulerLock(name = "orderExpiryScheduler", lockAtMostFor = "PT10M", lockAtLeastFor = "PT4M")
    @Transactional
    public void expireStaleOrders() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES);
        int expired = orderRepository.expireStaleOrders(
            OrderStatus.CREATED, threshold, OrderStatus.EXPIRED);
        if (expired > 0) {
            log.info("[OrderExpiryScheduler] Expired {} stale orders older than {} minutes",
                expired, STALE_THRESHOLD_MINUTES);
        }
    }
}
