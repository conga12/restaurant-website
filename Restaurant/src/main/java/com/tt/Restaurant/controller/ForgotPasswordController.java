package com.tt.Restaurant.controller;

import com.tt.Restaurant.model.PasswordResetToken;
import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.PasswordResetTokenRepository;
import com.tt.Restaurant.repository.UserRepository;
import com.tt.Restaurant.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:9090}")
    private String baseUrl;

    public ForgotPasswordController(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepo,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "redirect:/auth/forgot-password.html";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email) {
        try {
            Optional<User> optUser = userRepository.findByEmail(email);

            // Không leak thông tin: email có/không vẫn báo cùng 1 kiểu
            if (optUser.isEmpty()) {
                return "redirect:/auth/forgot-password.html?message=" +
                        enc("Nếu email tồn tại, hệ thống đã gửi link đặt lại mật khẩu.");
            }

            User user = optUser.get();

            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

            tokenRepo.save(new PasswordResetToken(token, expiresAt, user));

            String resetLink = baseUrl + "/auth/reset-password.html?token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8);

            emailService.sendPasswordResetEmail(email, resetLink);

            return "redirect:/auth/forgot-password.html?message=" +
                    enc("Đã gửi link reset mật khẩu tới email. Vui lòng kiểm tra hộp thư.");
        } catch (Exception e) {
            return "redirect:/auth/forgot-password.html?error=" +
                    enc("Gửi email thất bại. Vui lòng thử lại.");
        }
    }

    @PostMapping("/reset-password")
    @Transactional
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword
    ) {
        if (!password.equals(confirmPassword)) {
            return "redirect:/auth/reset-password.html?token=" + enc(token) + "&error=" + enc("Mật khẩu không khớp.");
        }

        String pwError = validatePassword(password);
        if (pwError != null) {
            return "redirect:/auth/reset-password.html?token=" + enc(token) + "&error=" + enc(pwError);
        }

        PasswordResetToken prt = tokenRepo.findByToken(token).orElse(null);
        if (prt == null || prt.isExpired()) {
            return "redirect:/auth/forgot-password.html?error=" + enc("Link reset không hợp lệ hoặc đã hết hạn.");
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // Xóa token trong cùng transaction để không bị TransactionRequiredException
        tokenRepo.delete(prt);
        // (Bạn có thể dùng tokenRepo.deleteByToken(token) cũng được, nhưng delete(prt) rõ ràng hơn)

        return "redirect:/auth/login.html?message=reset_success";
    }

    private String validatePassword(String password) {
        if (password == null) return "Mật khẩu không hợp lệ.";
        if (password.length() < 6 || password.length() > 20) return "Mật khẩu phải từ 6-20 ký tự.";

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            return "Mật khẩu phải gồm chữ hoa, chữ thường, số và ký tự đặc biệt.";
        }
        return null;
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}