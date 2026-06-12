package com.bepa.eis.server.api.external.virk.cvr;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Konfiguration for opslag mod cvrapi.dk.
 */
public class CvrApiConfig {

    private static final String DEFAULT_BASE_URL = "https://cvrapi.dk/api";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final URI baseUri;
    private final Duration timeout;
    private final String userAgent;

    public CvrApiConfig(URI baseUri, Duration timeout, String userAgent) {
        this.baseUri = Objects.requireNonNull(baseUri, "baseUri must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent must not be null");
    }

    public static CvrApiConfig defaultConfig() {
        return new CvrApiConfig(
                URI.create(DEFAULT_BASE_URL),
                DEFAULT_TIMEOUT,
                "EIS-server/1.0"
        );
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public String getUserAgent() {
        return userAgent;
    }
}