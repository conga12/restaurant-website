package com.tt.Restaurant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private User user;

    public PasswordResetToken() {}

    public PasswordResetToken(String token, LocalDateTime expiresAt, User user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}