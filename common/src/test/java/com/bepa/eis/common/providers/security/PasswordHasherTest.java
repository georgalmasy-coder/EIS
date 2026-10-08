package com.bepa.eis.common.providers.security;

/** Standalone regression suite, including a vector generated independently with .NET. */
public final class PasswordHasherTest {
    public static void main(String[] args) {
        String vector = "pbkdf2-sha256$600000$AAECAwQFBgcICQoLDA0ODw==$"
                + "8KvJXx+EDO7g/Ldy+Asb5ShCRFLjxpltkXPOTnuyNcY=";
        check(PasswordHasher.verify(vector, "Known-password-123"), "Independent PBKDF2 vector");
        check(!PasswordHasher.verify(vector, "Known-password-124"), "Incorrect password");
        check(!PasswordHasher.verify(vector, vector), "A hash cannot be used as its password");

        String password = "  Æøå-密碼-🔒  ";
        String first = PasswordHasher.hash(password);
        String second = PasswordHasher.hash(password);
        check(!first.equals(second), "Every password gets a fresh salt");
        check(first.length() == 90, "Hash fits the database constraint");
        check(PasswordHasher.isEncodedHash(first), "Migration recognizes existing hashes");
        check(PasswordHasher.verify(first, password), "Unicode and spaces survive hashing");
        check(!PasswordHasher.verify(first, password.trim()), "Passwords are not trimmed");
        check(!PasswordHasher.verify("plaintext", "plaintext"), "No plaintext fallback");

        String[] invalid = {
                "", "plaintext", "pbkdf2-sha256", vector + "$extra",
                vector.replace("600000", "0"), vector.replace("600000", "599999"),
                vector.replace("600000", "2000001"), vector.replace("600000", "2147483648"),
                vector.replace("600000", "0600000"), vector.replace("600000", "-1"),
                vector.replace("AAECAwQFBgcICQoLDA0ODw==", "AA=="),
                vector.replace("AAECAwQFBgcICQoLDA0ODw==", "not-base64"),
                vector.substring(0, vector.lastIndexOf('$') + 1) + "AA==",
                vector.substring(0, vector.length() - 1), "x".repeat(256)
        };
        for (String value : invalid) {
            check(!PasswordHasher.isEncodedHash(value), "Malformed hash rejected");
            check(!PasswordHasher.verify(value, "Known-password-123"), "Malformed hash cannot authenticate");
        }
        check(!PasswordHasher.verify(null, password), "Missing stored hash");
        check(!PasswordHasher.verify(vector, null), "Missing password");
        check(!PasswordHasher.verify(vector, " "), "Blank password");
        for (String value : new String[]{null, "", " \t\n"}) {
            try {
                PasswordHasher.hash(value);
                throw new AssertionError("Blank passwords must not be hashed");
            } catch (IllegalArgumentException expected) {
                // Accounts without passwords are stored as SQL NULL.
            }
        }
        System.out.println("PasswordHasher regression tests passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
