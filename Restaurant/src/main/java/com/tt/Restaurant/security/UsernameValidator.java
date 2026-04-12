package com.tt.Restaurant.security;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class UsernameValidator {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
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

    private static String normalize(String s) {
        if (s == null) return "";
        String trimmed = s.trim().toLowerCase();
        String nfd = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        String noDiacritics = DIACRITICS.matcher(nfd).replaceAll("");
        // bỏ hết ký tự không phải chữ/số để chống lách kiểu a-d_m!i@n
        return noDiacritics.replaceAll("[^a-z0-9]", "");
    }

    public static void validateOrThrow(String username) {
        String norm = normalize(username);

        if (norm.isBlank() || norm.length() < 3 || norm.length() > 30) {
            throw new IllegalArgumentException("Username không hợp lệ (3-30 ký tự).");
        }

        for (String word : RESERVED_WORDS) {
            if (norm.contains(word)) {
                throw new IllegalArgumentException("Tên người dùng không phù hợp. Vui lòng chọn tên khác.");
            }
        }
    }
}