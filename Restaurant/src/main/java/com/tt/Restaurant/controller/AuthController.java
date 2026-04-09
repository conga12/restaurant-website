package com.tt.Restaurant.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.tt.Restaurant.model.User;
import com.tt.Restaurant.service.UserService;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirectAttributes) {

        // Validate password match
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "password_mismatch");
            return "redirect:/auth/register";
        }

        // Validate password length
        if (password.length() < 6) {
            redirectAttributes.addAttribute("error", "password_short");
            return "redirect:/auth/register";
        }

        // Create user
        try {
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(password); // Hash in service!

            userService.register(user); // Gọi method register thay vì saveUser

            redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "user_exists");
            return "redirect:/auth/register";
        }
    }

    // ← THÊM: OAuth2 Register Endpoint
    @PostMapping("/oauth2/register")
    public String oauth2Register(HttpServletRequest request) {
        request.getSession().setAttribute("oauth2_mode", "register");
        return "redirect:/oauth2/authorization/google";
    }

    // ← THÊM: OAuth2 Login Endpoint
    @PostMapping("/oauth2/login")
    public String oauth2Login(HttpServletRequest request) {
        request.getSession().setAttribute("oauth2_mode", "login");
        return "redirect:/oauth2/authorization/google";
    }

    // ← THÊM: API endpoint để set OAuth2 mode (optional - for AJAX)
    @PostMapping("/api/oauth2/set-mode")
    @ResponseBody
    public Map<String, String> setOAuth2Mode(
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        String mode = body.get("mode");
        if (mode != null && (mode.equals("login") || mode.equals("register"))) {
            request.getSession().setAttribute("oauth2_mode", mode);
            return Map.of("status", "success");
        }
        return Map.of("status", "error");
    }
}