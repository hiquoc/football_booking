package com.project.common.outbox.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {

    private ExecutorService outboxPublisherExecutor;

    @Bean(destroyMethod = "")
    public ExecutorService outboxPublisherExecutor(OutboxProperties properties) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("outbox-publisher-" + sequence.getAndIncrement());
                return thread;
            }
        };
        this.outboxPublisherExecutor = Executors.newFixedThreadPool(properties.schedulerConcurrency(), threadFactory);
        return this.outboxPublisherExecutor;
    }

    @PreDestroy
    public void shutdownOutboxPublisherExecutor() {
        if (outboxPublisherExecutor == null) {
            return;
        }
        outboxPublisherExecutor.shutdown();
        try {
            if (!outboxPublisherExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Outbox publisher executor did not shut down within timeout");
                outboxPublisherExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            outboxPublisherExecutor.shutdownNow();
        }
    }
}
