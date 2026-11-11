package com.bepa.eis.server.api.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** Registered first in web.xml so rejected requests do not reach session/database work. */
public final class RequestRateLimitFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequestRateLimitFilter.class);
    private final RejectionWarnings rejectionWarnings = new RejectionWarnings();
    private RequestRateLimiter limiter;
    private int retryAfterSeconds;

    @Override
    public void init(FilterConfig config) throws ServletException {
        retryAfterSeconds = positive(config, "retryAfterSeconds", 60);
        limiter = new RequestRateLimiter(positive(config, "requestsPerSecond", 20),
                positive(config, "burstCapacity", 100),
                positive(config, "globalRequestsPerSecond", 500),
                positive(config, "globalBurstCapacity", 1000),
                positive(config, "maxTrackedClients", 10000), System::nanoTime);
    }

    private static int positive(FilterConfig config, String name, int fallback) throws ServletException {
        String value = config.getInitParameter(name);
        if (value == null) return fallback;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) return parsed;
        } catch (NumberFormatException ignored) {
        }
        throw new ServletException("Invalid positive integer for rate limit parameter " + name);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest req) || !(response instanceof HttpServletResponse resp)) {
            chain.doFilter(request, response);
            return;
        }
        // Never trust client-supplied Forwarded/X-Forwarded-For headers here.
        long retryAfter = limiter.retryAfter(req.getRemoteAddr());
        if (retryAfter == 0) {
            chain.doFilter(request, response);
            return;
        }
        long rejected = rejectionWarnings.record(System.nanoTime());
        if (rejected > 0) {
            log.warn("HTTP rate limit: {} requests rejected (429) since previous warning; "
                    + "warnings limited to one per 60 seconds per application instance", rejected);
        }
        resp.setStatus(429);
        resp.setHeader("Retry-After", Long.toString(Math.max(retryAfter, retryAfterSeconds)));
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
    }

    /** One shared counter across all IPs; no per-client log state or background task. */
    static final class RejectionWarnings {
        private static final long INTERVAL_NANOS = 60_000_000_000L;
        private boolean started;
        private long nextWarning;
        private long pending;

        // Synchronization prevents concurrent requests from producing duplicate warnings.
        synchronized long record(long now) {
            pending++;
            if (started && now - nextWarning < 0) return 0;
            started = true;
            nextWarning = now + INTERVAL_NANOS;
            long rejected = pending;
            pending = 0;
            return rejected;
        }
    }
}
