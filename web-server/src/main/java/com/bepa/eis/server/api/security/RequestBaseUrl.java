package com.bepa.eis.server.api.security;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Builds the external application URL from the request as interpreted by Tomcat. */
public final class RequestBaseUrl {
    private RequestBaseUrl() {}

    public static String from(HttpServletRequest request) {
        String scheme = request.getScheme().toLowerCase(Locale.ROOT);
        String host = request.getServerName();
        int port = request.getServerPort();
        String contextPath = request.getContextPath();
        if (!("https".equals(scheme) || "http".equals(scheme))
                || host == null || host.isBlank() || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid request origin");
        }
        if (contextPath == null) contextPath = "";
        if (!contextPath.isEmpty() && (!contextPath.startsWith("/") || contextPath.startsWith("//"))
                || contextPath.indexOf('?') >= 0 || contextPath.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Invalid application context path");
        }
        int explicitPort = ("https".equals(scheme) && port == 443)
                || ("http".equals(scheme) && port == 80) ? -1 : port;
        try {
            URI origin = new URI(scheme, null, host, explicitPort, null, null, null).parseServerAuthority();
            if (origin.getHost() == null || origin.getUserInfo() != null
                    || origin.getPort() != explicitPort
                    || (origin.getRawPath() != null && !origin.getRawPath().isEmpty())
                    || origin.getRawQuery() != null || origin.getRawFragment() != null) {
                throw new IllegalArgumentException("Invalid request host");
            }
            // getContextPath() is already URL-encoded; do not encode percent escapes twice.
            return new URI(origin.toASCIIString() + contextPath).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid request base URL", e);
        }
    }
}
