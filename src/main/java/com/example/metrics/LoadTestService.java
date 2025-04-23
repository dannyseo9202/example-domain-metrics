package com.example.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class LoadTestService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "http://localhost:8080";

    private final Random random = new Random();

    private final List<EndpointEntry> endpoints = List.of(
            new EndpointEntry("/orders/process?success=true", 60),
            new EndpointEntry("/orders/process?success=false", 10),
            new EndpointEntry("/orders/fail?reason=DB 에러", 5),
            new EndpointEntry("/orders/fail/inventory", 10),
            new EndpointEntry("/orders/fail/payment?reason=결제 시간 초과", 10),
            new EndpointEntry("/orders/fail/payment?reason=결제 거절", 3),
            new EndpointEntry("/orders/retry", 2)
    );

    public void doLoadTest(int threads, int requestsPerThread) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> runTestLoop(requestsPerThread));
        }

        shutdownExecutor(executor);
    }

    private void runTestLoop(int requestsPerThread) {
        for (int i = 0; i < requestsPerThread; i++) {
            String endpoint = pickEndpointByWeight();
            String url = BASE_URL + endpoint;

            try {
                Instant start = Instant.now();
                restTemplate.postForObject(url, null, String.class);
                Instant end = Instant.now();

                logSuccess(endpoint, Duration.between(start, end).toMillis());
            } catch (Exception e) {
                logFailure(endpoint, e);
            }

            sleepRandomDelay();
        }
    }

    private String pickEndpointByWeight() {
        int totalWeight = endpoints.stream().mapToInt(EndpointEntry::weight).sum();
        int r = random.nextInt(totalWeight);

        int cumulative = 0;
        for (EndpointEntry entry : endpoints) {
            cumulative += entry.weight();
            if (r < cumulative) {
                return entry.path();
            }
        }
        return endpoints.get(0).path(); // fallback
    }

    private void sleepRandomDelay() {
        try {
            Thread.sleep(100 + random.nextInt(200)); // 100~300ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Load test completed.");
    }

    private void logSuccess(String endpoint, long duration) {
        System.out.printf("[%s] %dms%n", endpoint, duration);
    }

    private void logFailure(String endpoint, Exception e) {
        System.err.printf("[%s] ERROR: %s%n", endpoint, e.getMessage());
    }

    private record EndpointEntry(String path, int weight) {}

    public void doLoadTestGradualFailure(int threads, int requestsPerThread) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            executor.submit(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    double ratio = (double) i / requestsPerThread;
                    double successProbability = 1.0 - 0.13 * Math.pow(ratio, 1.2);
                    boolean success = random.nextDouble() < successProbability;

                    String endpoint = success
                            ? "/orders/process?success=true"
                            : pickFailEndpoint();

                    String url = BASE_URL + endpoint;

                    try {
                        Instant start = Instant.now();
                        restTemplate.postForObject(url, null, String.class);
                        Instant end = Instant.now();

                        System.out.printf("Thread-%d [%s] %dms (%.2f%% success chance)%n",
                                threadIndex, endpoint, Duration.between(start, end).toMillis(), successProbability * 100);
                    } catch (Exception e) {
                        System.err.printf("Thread-%d [%s] ERROR: %s%n",
                                threadIndex, endpoint, e.getMessage());
                    }

                    sleepRandomDelay();
                }
            });
        }

        shutdownExecutor(executor);
    }

    private String pickFailEndpoint() {
        String[] failEndpoints = {
                "/orders/process?success=false",
                "/orders/fail?reason=DB 에러",
                "/orders/fail/inventory",
                "/orders/fail/payment?reason=결제 시간 초과",
                "/orders/fail/payment?reason=결제 거절"
        };
        return failEndpoints[random.nextInt(failEndpoints.length)];
    }
}
