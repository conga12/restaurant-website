package com.tt.Restaurant.util;

import java.util.HashSet;
import java.util.Set;

public class SecurityValidator {

    private static final Set<String> RESERVED_WORDS = new HashSet<>();

    static {
        // SQL Keywords
        RESERVED_WORDS.add("select");
        RESERVED_WORDS.add("insert");
        RESERVED_WORDS.add("update");
        RESERVED_WORDS.add("delete");
        RESERVED_WORDS.add("drop");
        RESERVED_WORDS.add("create");
        RESERVED_WORDS.add("alter");
        RESERVED_WORDS.add("exec");
        RESERVED_WORDS.add("execute");
        RESERVED_WORDS.add("union");
        RESERVED_WORDS.add("truncate");
        RESERVED_WORDS.add("grant");
        RESERVED_WORDS.add("revoke");

        // System Keywords
        RESERVED_WORDS.add("admin");
        RESERVED_WORDS.add("root");
        RESERVED_WORDS.add("system");
        RESERVED_WORDS.add("administrator");
        RESERVED_WORDS.add("password");
        RESERVED_WORDS.add("secret");
        RESERVED_WORDS.add("null");
        RESERVED_WORDS.add("true");
        RESERVED_WORDS.add("false");
        RESERVED_WORDS.add("or");
        RESERVED_WORDS.add("and");
    }

    /**
     * Validate username - Check for reserved words, SQL injection, etc.
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String lowerUsername = username.toLowerCase().trim();

        // Check length
        if (lowerUsername.length() < 3 || lowerUsername.length() > 50) {
            return false;
        }

        // Check reserved words
        if (RESERVED_WORDS.contains(lowerUsername)) {
            return false;
        }

        // Check for SQL injection patterns
        if (lowerUsername.contains("'") || lowerUsername.contains("\"") ||
                lowerUsername.contains(";") || lowerUsername.contains("--") ||
                lowerUsername.contains("/*") || lowerUsername.contains("*/") ||
                lowerUsername.contains("xp_") || lowerUsername.contains("sp_")) {
            return false;
        }

        // Check for allowed characters only (alphanumeric, underscore, dash)
        if (!lowerUsername.matches("^[a-z0-9_-]+$")) {
            return false;
        }

        return true;
    }

    /**
     * Get error message for invalid username
     */
    public static String getInvalidUsernameMessage(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username không được để trống";
        }

        String lowerUsername = username.toLowerCase().trim();

        if (lowerUsername.length() < 3) {
            return "Username phải có ít nhất 3 ký tự";
        }

        if (lowerUsername.length() > 50) {
            return "Username không được vượt quá 50 ký tự";
        }

        if (RESERVED_WORDS.contains(lowerUsername)) {
            return "Username '" + username + "' là từ khóa được bảo vệ. Vui lòng chọn username khác";
        }

        if (lowerUsername.contains("'") || lowerUsername.contains("\"") ||
                lowerUsername.contains(";") || lowerUsername.contains("--")) {
            return "Username không được chứa ký tự đặc biệt nguy hiểm";
        }

        if (!lowerUsername.matches("^[a-z0-9_-]+$")) {
            return "Username chỉ được chứa chữ, số, gạch ngang (-) và gạch dưới (_)";
        }

        return "Username không hợp lệ";
    }
}