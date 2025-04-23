package com.example.metrics;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderMetrics orderMetrics;

    public OrderService(OrderMetrics orderMetrics) {
        this.orderMetrics = orderMetrics;
    }

    public void processOrder(boolean isSuccess) {
        if (isSuccess) {
            orderMetrics.recordSuccess(this::simulateProcessing);
        } else {
            recordFailure("PG 승인 오류");
        }
    }

    public void retryOrder() {
        orderMetrics.recordRetry();
    }

    public void failDueToInventory() {
        recordFailure("재고 부족");
    }

    public void failDueToPayment(String reason) {
        recordFailure(reason);
    }

    private void recordFailure(String reason) {
        orderMetrics.recordFailure(this::simulateProcessing, reason);
    }

    private void simulateProcessing() {
        try {
            Thread.sleep(100); // 처리 시간 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
