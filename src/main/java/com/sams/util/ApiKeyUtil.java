package com.sams.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Generates API keys and hashes them (only the hash is stored in the database). */
public final class ApiKeyUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiKeyUtil() { }

    /** Example: sams_k3J9x...  (32 random bytes, URL-safe Base64). */
    public static String generate() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return "sams_" + Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String prefix(String key) {
        return key.substring(0, Math.min(12, key.length()));
    }

    public static String sha256(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
