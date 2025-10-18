package com.intellidesk.cognitia.userandauth.config;

import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        log.info("[AsyncConfig] [getAsyncExecutor] creating custom executor to copy tenant context");

        // Wrap every submitted Runnable to copy tenant context
        executor.setTaskDecorator(runnable -> {
            UUID tenantId = TenantContext.getTenantId();
            return () -> {
                try {
                    TenantContext.setTenantId(tenantId);
                    runnable.run();
                } finally {
                    TenantContext.clear();
                }
            };
        });

        executor.initialize();
        return executor;
    }
}
