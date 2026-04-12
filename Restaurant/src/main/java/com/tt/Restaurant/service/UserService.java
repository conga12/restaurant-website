package com.tt.Restaurant.service;

import com.tt.Restaurant.model.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User createUser(User user);
    User updateUser(Long id, User user);
    void deleteUser(Long id);

    User register(User user) throws Exception;

    // ← THÊM: Tìm user by email
    Optional<User> findByEmail(String email);

    // ← THÊM: Tìm user by username
    Optional<User> findByUsername(String username);
}