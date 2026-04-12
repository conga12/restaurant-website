package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.UserRepository;
import com.tt.Restaurant.service.UserService;
import com.tt.Restaurant.util.SecurityValidator;  // ← THÊM
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        // ← THÊM: Validate username
        if (!SecurityValidator.isValidUsername(user.getUsername())) {
            throw new RuntimeException(SecurityValidator.getInvalidUsernameMessage(user.getUsername()));
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User user) {
        // ← THÊM: Validate username
        if (!SecurityValidator.isValidUsername(user.getUsername())) {
            throw new RuntimeException(SecurityValidator.getInvalidUsernameMessage(user.getUsername()));
        }

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

    @Override
    public User register(User user) throws Exception {
        System.out.println("=== REGISTER START ===");
        System.out.println("Email: " + user.getEmail());
        System.out.println("Username: " + user.getUsername());

        // Check if email exists
        boolean emailExists = userRepository.existsByEmail(user.getEmail());
        System.out.println("Email exists: " + emailExists);

        if (emailExists) {
            System.out.println("Email đã tồn tại - Throwing exception");
            throw new Exception("Email đã tồn tại");
        }

        // Check if username exists
        boolean usernameExists = userRepository.existsByUsername(user.getUsername());
        System.out.println("Username exists: " + usernameExists);

        if (usernameExists) {
            System.out.println("Username đã tồn tại - Throwing exception");
            throw new Exception("Username đã tồn tại");
        }

        // Hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        System.out.println("Password encoded");

        // Save
        User savedUser = userRepository.save(user);
        System.out.println("User saved, ID: " + savedUser.getId());
        System.out.println("=== REGISTER END ===");

        return savedUser;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}