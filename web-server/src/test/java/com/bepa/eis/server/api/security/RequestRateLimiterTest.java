package com.bepa.eis.server.api.security;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Standalone regression tests with a deterministic monotonic clock. */
public final class RequestRateLimiterTest {
    public static void main(String[] args) throws Exception {
        AtomicLong clock = new AtomicLong();
        RequestRateLimiter limiter = new RequestRateLimiter(2, 2, 100, 100, 10, clock::get);
        expect(0, limiter.retryAfter("a"));
        expect(0, limiter.retryAfter("a"));
        expect(1, limiter.retryAfter("a"));
        expect(0, limiter.retryAfter("b")); // independent client
        clock.set(499_000_000L);
        expect(1, limiter.retryAfter("a"));
        clock.set(500_000_000L);
        expect(0, limiter.retryAfter("a"));
        expect(1, limiter.retryAfter("a"));

        clock.set(0);
        limiter = new RequestRateLimiter(1, 1, 1, 2, 10, clock::get);
        expect(0, limiter.retryAfter("a"));
        expect(1, limiter.retryAfter("a")); // rejection must not drain global budget
        expect(0, limiter.retryAfter("b"));
        expect(1, limiter.retryAfter("c")); // global limit across clients
        clock.set(1_000_000_000L);
        expect(0, limiter.retryAfter("c"));

        clock.set(0);
        limiter = new RequestRateLimiter(1, 2, 100, 100, 1, clock::get);
        expect(0, limiter.retryAfter("a"));
        expect(1, limiter.retryAfter("b")); // bounded registry
        expect(0, limiter.retryAfter("a")); // existing client still works
        clock.set(1_000_000_000L);
        expect(1, limiter.retryAfter("b")); // partially refilled bucket retained
        clock.set(2_000_000_000L);
        expect(0, limiter.retryAfter("b")); // fully refilled bucket reclaimed

        RequestRateLimiter concurrent = new RequestRateLimiter(1, 37, 100, 100, 10, () -> 0);
        AtomicInteger admitted = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try { start.await(); } catch (InterruptedException e) { throw new AssertionError(e); }
                for (int j = 0; j < 20; j++) {
                    if (concurrent.retryAfter("shared") == 0) admitted.incrementAndGet();
                }
            });
            threads[i].start();
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        expect(37, admitted.get());
        System.out.println("Request rate limiter regression tests passed.");
    }

    private static void expect(long expected, long actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
