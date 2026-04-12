package com.tt.Restaurant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "User", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "uk_user_email"),
        @UniqueConstraint(columnNames = "username", name = "uk_user_username")
})
public class User {

    public enum Role {
        ADMIN, STAFF, CUSTOMER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)  // ← THÊM: unique = true
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }

    public boolean isActive() {
        return this.id != null;
    }
}