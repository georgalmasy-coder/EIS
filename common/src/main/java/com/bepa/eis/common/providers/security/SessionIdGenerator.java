package com.bepa.eis.common.providers.security;

import java.security.SecureRandom;
import java.util.Base64;

public final class SessionIdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionIdGenerator() { }

    public static String create() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean isValid(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{43}");
    }
}
