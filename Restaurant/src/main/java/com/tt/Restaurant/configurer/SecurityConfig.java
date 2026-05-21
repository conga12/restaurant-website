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
                        .requestMatchers("/api/chat/**").permitAll()
                        .requestMatchers("/api/ai/**").permitAll()
                        .requestMatchers("/api/customer/orders/**").permitAll()

                        .requestMatchers("/user/**").permitAll()
                        .requestMatchers("/ws-notify/**").permitAll()


                        // =========================
                        // ADMIN-ONLY APIs (cấm STAFF)
                        // =========================
                        .requestMatchers("/admin/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("/admin/api/dishes/**").hasRole("ADMIN")
                        // nếu có upload ảnh danh mục/món:
                        .requestMatchers("/admin/api/upload/**").hasRole("ADMIN")

                        // =========================
                        // STAFF + ADMIN APIs (vận hành)
                        // bạn đổi theo đúng API thực tế của bạn
                        // =========================
                        .requestMatchers("/admin/api/orders/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/api/reservations/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/api/tables/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/api/payments/**").hasAnyRole("ADMIN", "STAFF")

                        // =========================
                        // Admin pages (UI) - cho cả ADMIN và STAFF
                        // =========================
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")

                        // Public APIs
                        .requestMatchers(HttpMethod.POST, "/api/customer/reservations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders/from-qr").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dishes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer/tables/**").permitAll()
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/payment/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews", "/api/reviews/recent", "/api/reviews/stats", "/api/reviews/distribution").permitAll()
                        .requestMatchers("/api/reviews/**").hasRole("CUSTOMER")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login.html")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/user/index.html", false)
                        .successHandler((request, response, authentication) -> {
                            String redirect = request.getParameter("redirect");
                            String role = authentication.getAuthorities().stream()
                                    .findFirst()
                                    .map(a -> a.getAuthority())
                                    .orElse("ROLE_CUSTOMER");

                            if (role.equals("ROLE_ADMIN")) {
                                response.sendRedirect("/admin/index.html");
                            } else if (role.equals("ROLE_STAFF")) {
                                response.sendRedirect("/admin/index.html");
                                // hoặc: response.sendRedirect("/admin/forms/order.html");
                            } else {
                                if (redirect != null && redirect.startsWith("/")) {
                                    response.sendRedirect(redirect);
                                } else {
                                    response.sendRedirect("/user/index.html");
                                }
                            }
                        })
                        .failureUrl("/auth/login.html?error")
                        .permitAll()
                )
                // ← OAUTH2 LOGIN
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth/login.html")
                        .successHandler((request, response, authentication) -> {
                            String redirect = (String) request.getSession().getAttribute("afterLoginRedirect");
                            request.getSession().removeAttribute("afterLoginRedirect");
                            String role = authentication.getAuthorities().stream()
                                    .findFirst()
                                    .map(a -> a.getAuthority())
                                    .orElse("ROLE_CUSTOMER");

                            request.getSession().removeAttribute("oauth2_mode");

                            if (role.equals("ROLE_ADMIN")) {
                                response.sendRedirect("/admin/index.html");
                            } else if (role.equals("ROLE_STAFF")) {
                                response.sendRedirect("/admin/index.html");
                                // hoặc: response.sendRedirect("/admin/forms/order.html");
                            } else {
                                if (redirect != null && redirect.startsWith("/")) {
                                    response.sendRedirect(redirect);
                                } else {
                                    response.sendRedirect("/user/index.html");
                                }
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            String mode = (String) request.getSession().getAttribute("oauth2_mode");

                            // Default message theo mode
                            String errorMessage = "register".equals(mode)
                                    ? "Đăng ký thất bại!"
                                    : "Đăng nhập thất bại!";

                            // LẤY MESSAGE TỪ SESSION DO CustomOAuth2UserService SET
                            String sessionMessage = (String) request.getSession().getAttribute("oauth2_error_message");
                            if (sessionMessage != null && !sessionMessage.isBlank()) {
                                errorMessage = sessionMessage;
                                request.getSession().removeAttribute("oauth2_error_message");
                                request.getSession().removeAttribute("oauth2_error_code");
                            }

                            try {
                                String encodedError = java.net.URLEncoder.encode(
                                        errorMessage,
                                        java.nio.charset.StandardCharsets.UTF_8
                                );

                                // QUAN TRỌNG: dùng chung param `error` cho cả login & register
                                if ("register".equals(mode)) {
                                    response.sendRedirect("/auth/register.html?error=" + encodedError);
                                } else {
                                    response.sendRedirect("/auth/login.html?error=" + encodedError);
                                }
                            } catch (Exception e) {
                                if ("register".equals(mode)) {
                                    response.sendRedirect("/auth/register.html?error=Đăng%20ký%20thất%20bại");
                                } else {
                                    response.sendRedirect("/auth/login.html?error=Đăng%20nhập%20thất%20bại");
                                }
                            } finally {
                                // dọn để tránh dính mode cũ sang lần sau
                                request.getSession().removeAttribute("oauth2_mode");
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

    @Bean
    public com.tt.Restaurant.chat.ChatSessionStore chatSessionStore() {
        return new com.tt.Restaurant.chat.ChatSessionStore();
    }
}