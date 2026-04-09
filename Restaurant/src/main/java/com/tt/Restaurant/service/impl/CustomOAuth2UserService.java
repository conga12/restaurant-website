package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.exception.OAuth2ValidationException;
import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            processOAuth2User(oAuth2User);
            return oAuth2User;
        } catch (OAuth2ValidationException ex) {
            // ← Throw với format: "message|errorCode"
            String fullMessage = ex.getMessage() + "|" + ex.getErrorCode();
            System.out.println("DEBUG CustomOAuth2UserService: Throwing OAuth2ValidationException with message: " + fullMessage);
            throw new OAuth2AuthenticationException(fullMessage);
        } catch (RuntimeException ex) {
            System.out.println("DEBUG CustomOAuth2UserService: RuntimeException - " + ex.getMessage());
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private void processOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        String mode = "login";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String sessionMode = (String) attrs.getRequest().getSession().getAttribute("oauth2_mode");
                if (sessionMode != null && !sessionMode.isEmpty()) {
                    mode = sessionMode;
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting mode - " + e.getMessage());
        }

        System.out.println("DEBUG: Mode = " + mode + ", Email = " + email);

        User existingUser = userRepository.findByEmail(email).orElse(null);
        System.out.println("DEBUG: Existing user found = " + (existingUser != null));

        // ← REGISTER MODE
        if ("register".equals(mode)) {
            System.out.println("DEBUG: REGISTER MODE - checking if email exists");
            if (existingUser != null) {
                System.out.println("DEBUG: Email already exists - throwing exception");
                throw new OAuth2ValidationException(
                        "Email này đã được đăng ký! Vui lòng sử dụng email khác hoặc đăng nhập.",
                        "REGISTER_EMAIL_EXISTS"
                );
            }

            System.out.println("DEBUG: Creating new user");
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(name != null ? name : email.split("@")[0]);
            newUser.setPassword("");
            newUser.setRole(User.Role.CUSTOMER);
            userRepository.save(newUser);
            System.out.println("DEBUG: New user created successfully");
        }
        // ← LOGIN MODE
        else {
            System.out.println("DEBUG: LOGIN MODE - checking if email exists");
            if (existingUser == null) {
                System.out.println("DEBUG: Email not found - throwing exception");
                throw new OAuth2ValidationException(
                        "Email này chưa đăng ký. Vui lòng đăng ký tài khoản trước!",
                        "LOGIN_EMAIL_NOT_FOUND"
                );
            }
            System.out.println("DEBUG: User found - login success");
        }
    }
}