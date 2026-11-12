package com.bepa.eis.server.api.security;

/** Deterministic warning throttle tests; no waiting or log file writes needed. */
public final class RequestRateLimitWarningsTest {
    public static void main(String[] args) throws Exception {
        var warnings = new RequestRateLimitFilter.RejectionWarnings();
        expect(1, warnings.record(0));
        Thread[] threads = new Thread[8];
        var emitted = new java.util.concurrent.atomic.AtomicLong();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) emitted.addAndGet(warnings.record(1));
            });
            threads[i].start();
        }
        for (Thread thread : threads) thread.join();
        expect(0, emitted.get());
        expect(0, warnings.record(59_999_999_999L));
        expect(8002, warnings.record(60_000_000_000L));
        expect(0, warnings.record(60_000_000_001L));
        expect(2, warnings.record(120_000_000_000L));
        System.out.println("Request rate limit warning tests passed.");
    }

    private static void expect(long expected, long actual) {
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
