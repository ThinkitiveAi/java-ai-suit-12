package com.healthfirst.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordUtils {

    private static final int BCRYPT_ROUNDS = 12; // >= 12 rounds for security
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordUtils() {
        this.passwordEncoder = new BCryptPasswordEncoder(BCRYPT_ROUNDS, new SecureRandom());
    }

    /**
     * Hash a password using BCrypt with 12 salt rounds
     * @param plainPassword The plain text password
     * @return The hashed password
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verify a password against its hash
     * @param plainPassword The plain text password
     * @param hashedPassword The hashed password
     * @return true if passwords match, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Check if a password meets the security requirements
     * @param password The password to validate
     * @return true if password meets requirements, false otherwise
     */
    public boolean isValidPassword(String password) {
        if (password == null) return false;
        
        // At least 8 characters, one uppercase, one lowercase, one digit, one special character
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(passwordPattern);
    }

    /**
     * Generate a secure random token
     * @param length The length of the token
     * @return A secure random token
     */
    public String generateSecureToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return token.toString();
    }
} 