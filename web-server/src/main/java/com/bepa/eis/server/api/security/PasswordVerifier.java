package com.bepa.eis.server.api.security;

import com.bepa.eis.common.providers.security.PasswordHasher;

public final class PasswordVerifier {
    private PasswordVerifier() {}

    /** Verifies the shared, versioned PBKDF2 password format. */
    public static boolean verifyPbkdf2Like(String storedHash, String password) {
        return PasswordHasher.verify(storedHash, password);
    }
}
