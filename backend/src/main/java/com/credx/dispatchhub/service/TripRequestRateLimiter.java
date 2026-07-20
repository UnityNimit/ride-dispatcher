package com.credx.dispatchhub.service;

import com.credx.dispatchhub.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-process per-rider rate limiter for trip creation.
 * Suitable for a single JVM; replace with Redis for multi-instance deployments.
 */
@Component
public class TripRequestRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MS = 60_000L;

    private final Map<Long, Deque<Long>> requestsByRider = new ConcurrentHashMap<>();

    public void checkOrThrow(Long riderId) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestsByRider.computeIfAbsent(riderId, id -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_MS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                throw new RateLimitExceededException(
                        "Too many trip requests. Please wait before requesting another ride.");
            }
            timestamps.addLast(now);
        }
    }
}
