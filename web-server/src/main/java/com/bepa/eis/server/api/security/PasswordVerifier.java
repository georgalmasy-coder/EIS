package com.bepa.eis.server.api.security;

public final class PasswordVerifier {
    private PasswordVerifier() {}

    /**
     * Placeholder: implementér efter jeres faktiske format.
     * Eksempel: PBKDF2: algo:iterations:saltBase64:hashBase64
     */
    public static boolean verifyPbkdf2Like(String storedHash, String password) {
        if (storedHash == null || storedHash.isBlank()) return false;

        // TODO: implementér korrekt verifikation (PBKDF2/BCrypt/Argon2)
        // Returnér ALDRIG true her i produktion uden rigtig verifikation.
        return false;
    }
}