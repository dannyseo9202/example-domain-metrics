package com.example.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderMetrics {

    private final MeterRegistry registry;

    private final Counter successCounter;
    private final Counter totalCounter;
    private final Counter retryCounter;
    private final AtomicInteger pendingOrders;
    private final Timer processTimer;

    // 캐싱된 실패 카운터 (동적 태그를 위한)
    private final Map<String, Counter> failureCounters = new ConcurrentHashMap<>();

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.successCounter = registry.counter("order_success_count");
        this.totalCounter = registry.counter("order_total_count");
        this.retryCounter = registry.counter("order_retry_count");
        this.pendingOrders = registry.gauge("order_pending_count", new AtomicInteger(0));
        this.processTimer = registry.timer("order_process_time");

        // 비율 게이지
        Gauge.builder("order_success_ratio", () -> {
            double success = successCounter.count();
            double total = totalCounter.count();
            return total > 0 ? success / total : 0.0;
        }).register(registry);
    }

    public void recordSuccess(Runnable task) {
        recordWithCounter(task, successCounter);
    }

    public void recordFailure(Runnable task, String reason) {
        recordWithFailureReason(task, reason);
    }

    public void recordRetry() {
        retryCounter.increment();
    }

    private void recordWithCounter(Runnable task, Counter resultCounter) {
        try {
            pendingOrders.incrementAndGet();
            totalCounter.increment();

            processTimer.record(() -> {
                resultCounter.increment();
                task.run();
            });
        } finally {
            pendingOrders.decrementAndGet();
        }
    }

    private void recordWithFailureReason(Runnable task, String reason) {
        try {
            pendingOrders.incrementAndGet();
            totalCounter.increment();

            processTimer.record(() -> {
                getFailureCounter(reason).increment();
                task.run();
            });
        } finally {
            pendingOrders.decrementAndGet();
        }
    }

    private Counter getFailureCounter(String reason) {
        return failureCounters.computeIfAbsent(reason, r ->
                Counter.builder("order_failure_count")
                        .tag("reason", r)
                        .register(registry)
        );
    }
}
