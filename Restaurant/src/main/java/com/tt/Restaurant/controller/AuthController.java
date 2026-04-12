package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.User;
import com.tt.Restaurant.security.UsernameValidator;
import com.tt.Restaurant.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // Đồng bộ với static pages
    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/auth/login.html";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/auth/register.html";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword) {

        // password mismatch
        if (!password.equals(confirmPassword)) {
            return "redirect:/auth/register.html?error=password_mismatch";
        }
        // password policy: 6-20, có chữ hoa + chữ thường + số + ký tự đặc biệt
        String pwError = validatePassword(password);
        if (pwError != null) {
            return "redirect:/auth/register.html?error=" +
                    java.net.URLEncoder.encode(pwError, java.nio.charset.StandardCharsets.UTF_8);
        }

        // ✅ chặn username nhạy cảm / reserved words
        try {
            UsernameValidator.validateOrThrow(username);
        } catch (IllegalArgumentException ex) {
            String encoded = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/auth/register.html?error=" + encoded;
        }

        try {
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(User.Role.CUSTOMER);

            userService.register(user);

            // success -> về login + message
            return "redirect:/auth/login.html?message=register_success";
        } catch (Exception e) {
            System.out.println("=== REGISTER ERROR ===");
            System.out.println("Error: " + e.getMessage());
            System.out.println("=== END ===");

            String msg = (e.getMessage() == null || e.getMessage().isBlank())
                    ? "Đăng ký thất bại. Vui lòng thử lại."
                    : e.getMessage();

            String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);
            return "redirect:/auth/register.html?error=" + encoded;
        }

    }

    @PostMapping("/oauth2/register")
    public String oauth2Register(HttpServletRequest request) {
        request.getSession().setAttribute("oauth2_mode", "register");
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/oauth2/login")
    public String oauth2Login(HttpServletRequest request) {
        request.getSession().setAttribute("oauth2_mode", "login");
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/api/oauth2/set-mode")
    @ResponseBody
    public Map<String, String> setOAuth2Mode(@RequestBody Map<String, String> body,
                                             HttpServletRequest request) {
        String mode = body.get("mode");
        if (mode != null && (mode.equals("login") || mode.equals("register"))) {
            request.getSession().setAttribute("oauth2_mode", mode);
            return Map.of("status", "success");
        }
        return Map.of("status", "error");
    }

    private String validatePassword(String password) {
        if (password == null) return "Mật khẩu không hợp lệ.";

        if (password.length() < 6 || password.length() > 20) {
            return "Mật khẩu phải từ 6-20 ký tự.";
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        // Ký tự đặc biệt: bất kỳ ký tự không phải chữ/số
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            return "Mật khẩu phải gồm chữ hoa, chữ thường, số và ký tự đặc biệt.";
        }

        return null; // OK
    }
}