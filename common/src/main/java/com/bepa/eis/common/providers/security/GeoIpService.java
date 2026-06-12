package com.bepa.eis.common.providers.security;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeoIpService {

    private static final String GEO_IP_API_URL = "https://ipwho.is/";
    private static final Duration GEO_IP_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CACHE_TTL = Duration.ofHours(12);

    private final HttpClient httpClient;
    private final Map<String, CachedGeoIpResult> cache = new ConcurrentHashMap<>();

    public GeoIpService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(GEO_IP_TIMEOUT)
                .build();
    }

    public GeoIpResult lookup(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return GeoIpResult.unknown();
        }

        String normalizedIpAddress = normalizeIpAddress(ipAddress);

        if (normalizedIpAddress == null || normalizedIpAddress.isBlank()) {
            return GeoIpResult.unknown();
        }

        if (isLocalAddress(normalizedIpAddress)) {
            return new GeoIpResult(
                    "LOCAL",
                    "Local network",
                    null,
                    null,
                    null,
                    null
            );
        }

        if (isPrivateIpv4Address(normalizedIpAddress)) {
            return new GeoIpResult(
                    "PRIVATE",
                    "Private network",
                    null,
                    null,
                    null,
                    null
            );
        }

        GeoIpResult cachedResult = getCachedResult(normalizedIpAddress);

        if (cachedResult != null) {
            return cachedResult;
        }

        GeoIpResult result = lookupExternal(normalizedIpAddress);
        cacheResult(normalizedIpAddress, result);

        return result;
    }

    private GeoIpResult lookupExternal(String ipAddress) {
        try {
            String encodedIpAddress = URLEncoder.encode(ipAddress, StandardCharsets.UTF_8);
            URI uri = URI.create(GEO_IP_API_URL + encodedIpAddress);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(GEO_IP_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() != 200) {
                return GeoIpResult.unknown();
            }

            return parseIpWhoIsResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GeoIpResult.unknown();
        } catch (IOException | RuntimeException e) {
            return GeoIpResult.unknown();
        }
    }

    private GeoIpResult parseIpWhoIsResponse(String json) {
        if (json == null || json.isBlank()) {
            return GeoIpResult.unknown();
        }

        Boolean success = extractJsonBoolean(json, "success");

        if (!Boolean.TRUE.equals(success)) {
            return GeoIpResult.unknown();
        }

        return new GeoIpResult(
                extractJsonString(json, "country_code"),
                extractJsonString(json, "country"),
                extractJsonString(json, "region"),
                extractJsonString(json, "city"),
                extractJsonDouble(json, "latitude"),
                extractJsonDouble(json, "longitude")
        );
    }

    private GeoIpResult getCachedResult(String ipAddress) {
        CachedGeoIpResult cachedResult = cache.get(ipAddress);

        if (cachedResult == null) {
            return null;
        }

        if (cachedResult.expiresAt().isBefore(Instant.now())) {
            cache.remove(ipAddress);
            return null;
        }

        return cachedResult.result();
    }

    private void cacheResult(String ipAddress, GeoIpResult result) {
        if (result == null) {
            return;
        }

        cache.put(
                ipAddress,
                new CachedGeoIpResult(
                        result,
                        Instant.now().plus(CACHE_TTL)
                )
        );
    }

    private String normalizeIpAddress(String ipAddress) {
        String value = ipAddress.trim();

        if (value.isBlank()) {
            return null;
        }

        /*
         * X-Forwarded-For can contain several IPs:
         * client, proxy1, proxy2
         *
         * If such a value reaches this service, use the first IP.
         */
        int commaIndex = value.indexOf(',');

        if (commaIndex >= 0) {
            value = value.substring(0, commaIndex).trim();
        }

        /*
         * IPv6 loopback sometimes appears in long form.
         */
        if ("0:0:0:0:0:0:0:1".equals(value)) {
            return "::1";
        }

        return value;
    }

    private boolean isLocalAddress(String ipAddress) {
        return "127.0.0.1".equals(ipAddress)
                || "::1".equals(ipAddress)
                || "localhost".equalsIgnoreCase(ipAddress);
    }

    private boolean isPrivateIpv4Address(String ipAddress) {
        return ipAddress.startsWith("10.")
                || ipAddress.startsWith("192.168.")
                || isPrivate172Address(ipAddress);
    }

    private boolean isPrivate172Address(String ipAddress) {
        if (!ipAddress.startsWith("172.")) {
            return false;
        }

        String[] parts = ipAddress.split("\\.");

        if (parts.length < 2) {
            return false;
        }

        try {
            int secondPart = Integer.parseInt(parts[1]);
            return secondPart >= 16 && secondPart <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String extractJsonString(String json, String key) {
        String rawValue = extractJsonValue(json, key);

        if (rawValue == null) {
            return null;
        }

        rawValue = rawValue.trim();

        if ("null".equals(rawValue)) {
            return null;
        }

        if (rawValue.length() >= 2 && rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
            return unescapeJsonString(rawValue.substring(1, rawValue.length() - 1));
        }

        return rawValue;
    }

    private Double extractJsonDouble(String json, String key) {
        String rawValue = extractJsonValue(json, key);

        if (rawValue == null || rawValue.isBlank() || "null".equals(rawValue.trim())) {
            return null;
        }

        try {
            return Double.valueOf(rawValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean extractJsonBoolean(String json, String key) {
        String rawValue = extractJsonValue(json, key);

        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalizedValue = rawValue.trim();

        if ("true".equalsIgnoreCase(normalizedValue)) {
            return Boolean.TRUE;
        }

        if ("false".equalsIgnoreCase(normalizedValue)) {
            return Boolean.FALSE;
        }

        return null;
    }

    private String extractJsonValue(String json, String key) {
        if (json == null || key == null) {
            return null;
        }

        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);

        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex);

        if (colonIndex < 0) {
            return null;
        }

        int valueStartIndex = colonIndex + 1;

        while (valueStartIndex < json.length()
                && Character.isWhitespace(json.charAt(valueStartIndex))) {
            valueStartIndex++;
        }

        if (valueStartIndex >= json.length()) {
            return null;
        }

        if (json.charAt(valueStartIndex) == '"') {
            return extractQuotedJsonValue(json, valueStartIndex);
        }

        int valueEndIndex = valueStartIndex;

        while (valueEndIndex < json.length()
                && json.charAt(valueEndIndex) != ','
                && json.charAt(valueEndIndex) != '}') {
            valueEndIndex++;
        }

        return json.substring(valueStartIndex, valueEndIndex).trim();
    }

    private String extractQuotedJsonValue(String json, int quoteStartIndex) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int i = quoteStartIndex + 1; i < json.length(); i++) {
            char current = json.charAt(i);

            if (escaped) {
                value.append('\\');
                value.append(current);
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                return "\"" + value + "\"";
            }

            value.append(current);
        }

        return null;
    }

    private String unescapeJsonString(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private record CachedGeoIpResult(
            GeoIpResult result,
            Instant expiresAt
    ) {
    }

    public record GeoIpResult(
            String countryCode,
            String countryName,
            String regionName,
            String city,
            Double latitude,
            Double longitude
    ) {
        public static GeoIpResult unknown() {
            return new GeoIpResult(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}