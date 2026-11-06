package com.sams.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with PBKDF2-HMAC-SHA256 (built into the JDK - no extra library).
 * Stored format:  pbkdf2$ITERATIONS$BASE64(SALT)$BASE64(HASH)
 */
public final class PasswordUtil {
    private static final int ITERATIONS = 65000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] dk = derive(password.toCharArray(), salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(dk);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        String[] p = stored.split("\\$");
        if (p.length != 4 || !"pbkdf2".equals(p[0])) return false;
        try {
            int iterations = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);   // constant-time comparison
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Minimum policy: 8+ characters with at least one letter and one digit. */
    public static String validateStrength(String pw) {
        if (pw == null || pw.length() < 8) return "Password must be at least 8 characters long.";
        boolean letter = false, digit = false;
        for (char c : pw.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        return (letter && digit) ? null : "Password must contain at least one letter and one digit.";
    }

    private static byte[] derive(char[] pw, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pw, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }
}
