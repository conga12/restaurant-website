package com.tt.Restaurant.configurer;

import com.tt.Restaurant.service.impl.CustomUserDetailsService;
import com.tt.Restaurant.service.impl.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(customUserDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authenticationProvider(authenticationProvider())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/register").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/login/oauth2/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()

                        .requestMatchers("/", "/index.html").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/admin/assets/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/api/ai/**").permitAll()

                        .requestMatchers("/user/**").permitAll()
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")

                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers(HttpMethod.POST, "/api/orders/from-qr").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dishes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer/tables/**").permitAll()
                        .requestMatchers("/api/payment/**").permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login.html")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            String role = authentication.getAuthorities().stream()
                                    .findFirst()
                                    .map(a -> a.getAuthority())
                                    .orElse("ROLE_CUSTOMER");

                            if (role.equals("ROLE_ADMIN")) {
                                response.sendRedirect("/admin/index.html");
                            } else if (role.equals("ROLE_STAFF")) {
                                response.sendRedirect("/staff/index.html");
                            } else {
                                response.sendRedirect("/user/index.html");
                            }
                        })
                        .failureUrl("/auth/login.html?error")
                        .permitAll()
                )
                // ← OAUTH2 LOGIN
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth/login.html")
                        .successHandler((request, response, authentication) -> {
                            String role = authentication.getAuthorities().stream()
                                    .findFirst()
                                    .map(a -> a.getAuthority())
                                    .orElse("ROLE_CUSTOMER");

                            request.getSession().removeAttribute("oauth2_mode");

                            if (role.equals("ROLE_ADMIN")) {
                                response.sendRedirect("/admin/index.html");
                            } else if (role.equals("ROLE_STAFF")) {
                                response.sendRedirect("/staff/index.html");
                            } else {
                                response.sendRedirect("/user/index.html");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            String errorMessage = "Đăng nhập thất bại!";
                            String errorCode = "UNKNOWN";

                            System.out.println("======== OAUTH2 FAILURE HANDLER ========");
                            System.out.println("Exception class: " + exception.getClass().getName());
                            System.out.println("Exception message: " + exception.getMessage());
                            if (exception.getCause() != null) {
                                System.out.println("Exception cause: " + exception.getCause().getMessage());
                            }

                            // ← Priority 1: Parse từ exception.getMessage()
                            if (exception.getMessage() != null && !exception.getMessage().isEmpty()) {
                                String fullMsg = exception.getMessage();
                                System.out.println("Full message from exception: " + fullMsg);

                                if (fullMsg.contains("|")) {
                                    String[] parts = fullMsg.split("\\|", 2);
                                    errorMessage = parts[0].trim();
                                    errorCode = parts[1].trim();
                                    System.out.println("Parsed - Message: " + errorMessage + ", Code: " + errorCode);
                                } else {
                                    errorMessage = fullMsg;
                                    System.out.println("No code separator found, using full message: " + errorMessage);
                                }
                            }

                            // ← Priority 2: Check cause
                            if (errorMessage.equals("Đăng nhập thất bại!") && exception.getCause() != null) {
                                String causeMsg = exception.getCause().getMessage();
                                System.out.println("Checking cause: " + causeMsg);
                                if (causeMsg != null && !causeMsg.isEmpty()) {
                                    if (causeMsg.contains("|")) {
                                        String[] parts = causeMsg.split("\\|", 2);
                                        errorMessage = parts[0].trim();
                                        errorCode = parts[1].trim();
                                    } else {
                                        errorMessage = causeMsg;
                                    }
                                }
                            }

                            System.out.println("Final - Error Message: " + errorMessage);
                            System.out.println("Final - Error Code: " + errorCode);
                            String mode = (String) request.getSession().getAttribute("oauth2_mode");
                            System.out.println("Mode: " + mode);
                            System.out.println("========================================");

                            try {
                                String encodedError = java.net.URLEncoder.encode(errorMessage, "UTF-8");

                                if ("register".equals(mode)) {
                                    String redirectUrl = "/auth/register.html?oauth2_error=" + encodedError + "&code=" + errorCode;
                                    System.out.println("Redirect to: " + redirectUrl);
                                    response.sendRedirect(redirectUrl);
                                } else {
                                    String redirectUrl = "/auth/login.html?oauth2_error=" + encodedError + "&code=" + errorCode;
                                    System.out.println("Redirect to: " + redirectUrl);
                                    response.sendRedirect(redirectUrl);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                response.sendRedirect("/auth/login.html?error=true");
                            }
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth/login.html?logout")
                        .permitAll()
                );

        return http.build();
    }
}