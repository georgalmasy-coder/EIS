package com.bepa.eis.server.api.security;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone filter tests without creating sessions or accessing a database. */
public final class RequestRateLimitFilterTest {
    public static void main(String[] args) throws Exception {
        RequestRateLimitFilter filter = new RequestRateLimitFilter();
        // A very slow refill makes the one-request burst deterministic during this test.
        filter.init(config(Map.of("requestsPerSecond", "1", "burstCapacity", "1",
                "retryAfterSeconds", "60")));
        AtomicInteger passed = new AtomicInteger();
        Map<String, String> responseValues = new HashMap<>();
        StringWriter body = new StringWriter();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (proxy, method, values) -> {
                    switch (method.getName()) {
                        case "setStatus" -> responseValues.put("status", values[0].toString());
                        case "setHeader" -> responseValues.put((String) values[0], (String) values[1]);
                        case "setContentType" -> responseValues.put("contentType", (String) values[0]);
                        case "getWriter" -> { return new PrintWriter(body); }
                        default -> throw new AssertionError("Unexpected response access: " + method.getName());
                    }
                    return null;
                });
        filter.doFilter(request("a"), response, (req, resp) -> passed.incrementAndGet());
        filter.doFilter(request("a"), response, (req, resp) -> passed.incrementAndGet());
        if (passed.get() != 1 || !"429".equals(responseValues.get("status"))
                || !"60".equals(responseValues.get("Retry-After"))
                || !"no-store".equals(responseValues.get("Cache-Control"))
                || !"application/json;charset=UTF-8".equals(responseValues.get("contentType"))
                || !body.toString().contains("Too many requests")) {
            throw new AssertionError("Rejected request must stop the chain and return HTTP 429: " + responseValues);
        }
        filter.doFilter(request("b"), response, (req, resp) -> passed.incrementAndGet());
        if (passed.get() != 2) throw new AssertionError("Different IP must have its own budget");
        filter.init(config(Map.of("requestsPerSecond", "1", "burstCapacity", "1",
                "retryAfterSeconds", "120")));
        filter.doFilter(request("a"), response, (req, resp) -> {});
        filter.doFilter(request("a"), response, (req, resp) -> {});
        if (!"120".equals(responseValues.get("Retry-After"))) {
            throw new AssertionError("Configured Retry-After must be returned");
        }
        filter.init(config(Map.of("requestsPerSecond", "1", "burstCapacity", "1")));
        filter.doFilter(request("a"), response, (req, resp) -> {});
        filter.doFilter(request("a"), response, (req, resp) -> {});
        if (!"60".equals(responseValues.get("Retry-After"))) {
            throw new AssertionError("Default Retry-After must be 60 seconds");
        }
        for (String invalid : new String[]{"0", "-1", "abc", "2147483648"}) {
            for (String parameter : new String[]{"requestsPerSecond", "retryAfterSeconds"}) {
                try {
                    new RequestRateLimitFilter().init(config(Map.of(parameter, invalid)));
                    throw new AssertionError("Invalid configuration accepted: " + invalid);
                } catch (ServletException expected) {
                    // Invalid limits must fail application startup.
                }
            }
        }
        new RequestRateLimitFilter().init(config(Map.of())); // defaults are valid
        System.out.println("Request rate limit filter regression tests passed.");
    }

    private static FilterConfig config(Map<String, String> parameters) {
        return (FilterConfig) Proxy.newProxyInstance(FilterConfig.class.getClassLoader(),
                new Class<?>[]{FilterConfig.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getInitParameter")) return parameters.get(args[0]);
                    throw new AssertionError("Unexpected config access: " + method.getName());
                });
    }

    private static HttpServletRequest request(String address) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getRemoteAddr")) return address;
                    // Header/session access fails: client headers cannot bypass the limiter.
                    throw new AssertionError("Unexpected request access: " + method.getName());
                });
    }
}
