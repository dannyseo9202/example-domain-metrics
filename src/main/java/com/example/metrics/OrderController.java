package com.example.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final LoadTestService loadTestService;

    @PostMapping("/process")
    public String processOrder(@RequestParam(defaultValue = "true") boolean success) {
        orderService.processOrder(success);
        return success
                ? "Order processed successfully."
                : "Order failed.";
    }

    @PostMapping("/retry")
    public String retryOrder() {
        orderService.retryOrder();
        return "🔁 Retry recorded.";
    }

    @PostMapping("/fail/payment")
    public String failDueToPayment(@RequestParam(defaultValue = "payment_fail") String reason) {
        orderService.failDueToPayment(reason);
        return String.format("Payment failure recorded. Reason: %s", reason);
    }

    @PostMapping("/fail/inventory")
    public String failDueToInventory() {
        orderService.failDueToInventory();
        return "Inventory failure recorded.";
    }

    @PostMapping("/fail")
    public String failWithReason(@RequestParam String reason) {
        orderService.failDueToPayment(reason);
        return String.format("Failure recorded with reason: %s", reason);
    }

    @GetMapping("/loadtest")
    public String loadTest(@RequestParam int threads, @RequestParam int requestsPerThread) {
        loadTestService.doLoadTest(threads, requestsPerThread);
        return String.format("Load test completed: %d threads × %d requests", threads, requestsPerThread);
    }

    @GetMapping("/loadtest/gradual")
    public String gradualLoadTest(@RequestParam int threads, @RequestParam int requestsPerThread) {
        loadTestService.doLoadTestGradualFailure(threads, requestsPerThread);
        return "Gradual failure load test started!";
    }

}
