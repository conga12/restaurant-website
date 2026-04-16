package com.tt.Restaurant.dto;

import com.tt.Restaurant.model.User;

public class UserRequestDTO {
    private String username;
    private String email;
    private String phone;
    private User.Role role;
    private String password; // raw password từ client

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public User.Role getRole() { return role; }
    public String getPassword() { return password; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(User.Role role) { this.role = role; }
    public void setPassword(String password) { this.password = password; }
}