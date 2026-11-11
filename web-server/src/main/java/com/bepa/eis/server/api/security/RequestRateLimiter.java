package com.bepa.eis.server.api.security;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Bounded, thread-safe token buckets. All limits apply to one application instance. */
final class RequestRateLimiter {
    private final Map<String, Bucket> clients = new HashMap<>();
    private final int clientRate, clientBurst, maxClients;
    private final Bucket global;
    private final LongSupplier clock;
    private long nextCleanup;

    RequestRateLimiter(int clientRate, int clientBurst, int globalRate, int globalBurst,
                       int maxClients, LongSupplier clock) {
        if (clientRate <= 0 || clientBurst <= 0 || globalRate <= 0 || globalBurst <= 0 || maxClients <= 0) {
            throw new IllegalArgumentException("Rate limits must be positive");
        }
        this.clientRate = clientRate;
        this.clientBurst = clientBurst;
        this.maxClients = maxClients;
        this.clock = clock;
        long now = clock.getAsLong();
        global = new Bucket(globalRate, globalBurst, now);
        nextCleanup = now;
    }

    /** Returns zero when admitted, otherwise the Retry-After delay in seconds. */
    synchronized long retryAfter(String address) {
        long now = clock.getAsLong();
        global.refill(now);
        if (global.tokens < 1) return global.retryAfter();

        Bucket client = clients.get(address);
        if (client == null) {
            if (clients.size() >= maxClients && now - nextCleanup >= 0) {
                clients.values().removeIf(bucket -> {
                    bucket.refill(now);
                    return bucket.tokens >= bucket.capacity;
                });
                nextCleanup = now + 1_000_000_000L;
            }
            if (clients.size() >= maxClients) return 1;
            client = new Bucket(clientRate, clientBurst, now);
            clients.put(address, client);
        }
        client.refill(now);
        if (client.tokens < 1) return client.retryAfter();
        client.tokens--;
        global.tokens--;
        return 0;
    }

    private static final class Bucket {
        final int rate, capacity;
        double tokens;
        long updated;

        Bucket(int rate, int capacity, long now) {
            this.rate = rate;
            this.capacity = capacity;
            tokens = capacity;
            updated = now;
        }

        void refill(long now) {
            tokens = Math.min(capacity, tokens + Math.max(0, now - updated) / 1_000_000_000.0 * rate);
            updated = now;
        }

        long retryAfter() {
            return Math.max(1, (long) Math.ceil((1 - tokens) / rate));
        }
    }
}
