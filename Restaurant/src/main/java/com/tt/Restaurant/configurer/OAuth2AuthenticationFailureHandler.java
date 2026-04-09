package com.tt.Restaurant.configurer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String errorMessage = "Đăng nhập thất bại!";

        // ← Lấy error message từ exception
        if (exception != null && exception.getMessage() != null) {
            errorMessage = exception.getMessage();
        }

        // ← Check nếu là OAuth2 error
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            errorMessage = exception.getCause().getMessage();
        }

        try {
            String encodedError = URLEncoder.encode(errorMessage, "UTF-8");
            response.sendRedirect("/auth/login.html?oauth2_error=" + encodedError);
        } catch (Exception e) {
            response.sendRedirect("/auth/login.html?error=true");
        }
    }
}