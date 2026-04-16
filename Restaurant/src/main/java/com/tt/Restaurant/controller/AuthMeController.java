package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class AuthMeController {

    private final UserRepository userRepository;

    public AuthMeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private List<String> splitModules(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    @GetMapping("/api/auth/me")
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null) {
            return Map.of("authenticated", false);
        }

        String role = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("ROLE_CUSTOMER");

        String email = authentication.getName();
        var userOpt = userRepository.findByEmail(email);
        String username = userOpt.map(User::getUsername).orElse(email);

        List<String> modules = List.of();

        if ("ROLE_ADMIN".equals(role)) {
            modules = List.of("DASHBOARD","CATEGORY","DISH","TABLE","RESERVATION","ORDER","PAYMENT","USER","REVIEW");
        } else if ("ROLE_STAFF".equals(role)) {


            modules = userOpt.map(u -> splitModules(u.getStaffModules())).orElse(List.of());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", true);
        result.put("email", email);
        result.put("username", username);
        result.put("role", role);
        result.put("modules", modules);
        return result;
    }
}