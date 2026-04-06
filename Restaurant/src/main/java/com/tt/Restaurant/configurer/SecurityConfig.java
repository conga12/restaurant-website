package com.tt.Restaurant.configurer;

import com.tt.Restaurant.service.impl.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
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

                        .requestMatchers("/", "/index.html").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/admin/assets/**").permitAll()

                        // QR public
                        .requestMatchers("/api/ai/**").permitAll()
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers("/user/qr-menu.html").permitAll()
                        .requestMatchers("/user/menu.html").permitAll()
                        .requestMatchers("/user/assets/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orders/from-qr").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dishes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/customer/tables/**").permitAll()

                        .requestMatchers("/user/reservation-history.html").hasRole("CUSTOMER")
                        .requestMatchers("/user/**").permitAll()
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")

                        .requestMatchers("/api/payment/**").permitAll()

                        .requestMatchers("/admin/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/forms/user.html").hasRole("ADMIN")
                        .requestMatchers("/admin/forms/reservation.html").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/api/reservations/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/api/tables/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/forms/table.html").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/forms/payment.html").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/api/payments/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers("/admin/assets/**").permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login.html")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                            boolean isStaff = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));

                            boolean isCustomer = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

                            if (isAdmin) {
                                response.sendRedirect("/admin/index.html");
                            } else if (isStaff) {
                                response.sendRedirect("/staff/index.html");
                            } else if (isCustomer) {
                                response.sendRedirect("/user/index.html");
                            } else {
                                response.sendRedirect("/auth/login.html?error");
                            }
                        })
                        .failureUrl("/auth/login.html?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth/login.html")
                        .permitAll()
                );

        return http.build();
    }
}