package com.bepa.eis.common.providers.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Versioned, salted password hashes. Plaintext is never accepted by verify. */
public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return PREFIX + "$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String storedHash, String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        EncodedHash encoded = parse(storedHash);
        return encoded != null && MessageDigest.isEqual(encoded.hash(),
                derive(password, encoded.salt(), encoded.iterations()));
    }

    public static boolean isEncodedHash(String value) {
        return parse(value) != null;
    }

    private static EncodedHash parse(String value) {
        if (value == null || value.length() > 255) {
            return null;
        }
        String[] parts = value.split("\\$", -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return null;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            // Bound work for corrupted or hostile values stored in the database.
            if (iterations < ITERATIONS || iterations > 2_000_000
                    || !Integer.toString(iterations).equals(parts[1])) {
                return null;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] hash = Base64.getDecoder().decode(parts[3]);
            if (salt.length != SALT_BYTES || hash.length != HASH_BYTES
                    || !Base64.getEncoder().encodeToString(salt).equals(parts[2])
                    || !Base64.getEncoder().encodeToString(hash).equals(parts[3])) {
                return null;
            }
            return new EncodedHash(iterations, salt, hash);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, HASH_BYTES * 8);
        Arrays.fill(chars, '\0');
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    private record EncodedHash(int iterations, byte[] salt, byte[] hash) {}
}
