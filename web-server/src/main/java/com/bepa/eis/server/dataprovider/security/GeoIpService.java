package com.bepa.eis.server.dataprovider.security;

public class GeoIpService {

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

        /*
         * This is the integration point for real GeoIP lookup.
         *
         * Recommended later implementations:
         * 1) MaxMind GeoLite2 local database
         * 2) Internal company IP range table
         * 3) External API, for example ipinfo.io, ip-api.com or Azure Maps
         *
         * Important:
         * GeoIP lookup must never block or break login.
         * If lookup fails, return GeoIpResult.unknown().
         */
        return GeoIpResult.unknown();
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