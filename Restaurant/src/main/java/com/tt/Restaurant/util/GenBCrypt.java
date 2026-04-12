package com.tt.Restaurant.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenBCrypt {
    public static void main(String[] args) {
        String raw = "1234567"; // đổi mật khẩu bạn muốn ở đây
        System.out.println(new BCryptPasswordEncoder().encode(raw));
    }
}