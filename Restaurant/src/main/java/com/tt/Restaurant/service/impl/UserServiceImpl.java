package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.UserRepository;
import com.tt.Restaurant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    @Override
    public User createUser(User user) {
        // Check if email exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Check if username exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        // Hash password
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User user) {
        User oldUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        oldUser.setUsername(user.getUsername());
        oldUser.setEmail(user.getEmail());
        oldUser.setPhone(user.getPhone());
        oldUser.setRole(user.getRole());

        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            oldUser.setPassword(user.getPassword());
        }

        return userRepository.save(oldUser);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // ← THÊM METHOD register() NÀY
    @Override
    public User register(User user) throws Exception {
        // Validate email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new Exception("Email không được để trống");
        }

        // Validate username
        if (user.getUsername() == null || user.getUsername().length() < 3) {
            throw new Exception("Username phải có ít nhất 3 ký tự");
        }

        // Validate password
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new Exception("Password phải có ít nhất 6 ký tự");
        }

        // Check if email exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new Exception("Email đã tồn tại");
        }

        // Check if username exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new Exception("Username đã tồn tại");
        }

        // Hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }
}