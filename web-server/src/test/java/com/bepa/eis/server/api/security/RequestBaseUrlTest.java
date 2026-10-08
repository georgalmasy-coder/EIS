package com.bepa.eis.server.api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;

/** Standalone origin/context tests, including proxy-adjusted Servlet request values. */
public final class RequestBaseUrlTest {
    public static void main(String[] args) {
        check("https", "localhost", 443, "", "https://localhost");
        check("https", "prod.example.dk", 443, "/eis", "https://prod.example.dk/eis");
        check("https", "test.example.dk", 8443, "/eis", "https://test.example.dk:8443/eis");
        check("http", "dev.example.dk", 80, "", "http://dev.example.dk");
        check("http", "localhost", 8080, "/test", "http://localhost:8080/test");
        check("https", "::1", 443, "", "https://[::1]");
        check("https", "[::1]", 8443, "/eis", "https://[::1]:8443/eis");
        check("HTTPS", "public.example.dk", 443, "/eis%20test", "https://public.example.dk/eis%20test");
        reject("ftp", "example.dk", 21, "");
        reject("https", "bad/host", 443, "");
        reject("https", "example.dk@evil.dk", 443, "");
        reject("https", "example.dk", 0, "");
        reject("https", "example.dk", 65536, "");
        reject("https", "example.dk", 443, "//evil.dk");
        reject("https", "example.dk", 443, "/eis?redirect=evil");
        System.out.println("Request base URL regression tests passed.");
    }

    private static void check(String scheme, String host, int port, String context, String expected) {
        String actual = RequestBaseUrl.from(request(scheme, host, port, context));
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private static void reject(String scheme, String host, int port, String context) {
        try {
            RequestBaseUrl.from(request(scheme, host, port, context));
            throw new AssertionError("Invalid request origin should be rejected");
        } catch (IllegalArgumentException expected) {}
    }

    private static HttpServletRequest request(String scheme, String host, int port, String context) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getScheme" -> scheme;
                    case "getServerName" -> host;
                    case "getServerPort" -> port;
                    case "getContextPath" -> context;
                    default -> throw new AssertionError("Unexpected request access: " + method.getName());
                });
    }
}
