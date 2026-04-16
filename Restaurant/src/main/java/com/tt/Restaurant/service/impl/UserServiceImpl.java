package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.StaffModulesDTO;
import com.tt.Restaurant.dto.UserRequestDTO;
import com.tt.Restaurant.dto.UserResponseDTO;
import com.tt.Restaurant.model.User;
import com.tt.Restaurant.repository.UserRepository;
import com.tt.Restaurant.service.UserService;
import com.tt.Restaurant.util.SecurityValidator;
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

    private UserResponseDTO toDto(User u) {
        return new UserResponseDTO(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                u.getCreatedAt()
        );
    }

    private String normalizeModules(List<String> modules) {
        if (modules == null) return null;
        return modules.stream()
                .filter(m -> m != null && !m.isBlank())
                .map(m -> m.trim().toUpperCase())
                .distinct()
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    private List<String> splitModules(String staffModules) {
        if (staffModules == null || staffModules.isBlank()) return List.of();
        return java.util.Arrays.stream(staffModules.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }
    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return toDto(u);
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new RuntimeException("Username không được để trống");
        }
        if (!SecurityValidator.isValidUsername(req.getUsername())) {
            throw new RuntimeException(SecurityValidator.getInvalidUsernameMessage(req.getUsername()));
        }
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }

        User u = new User();
        u.setUsername(req.getUsername().trim());
        u.setEmail(req.getEmail().trim());
        u.setPhone(req.getPhone());
        u.setRole(req.getRole() == null ? User.Role.CUSTOMER : req.getRole());
        u.setPassword(passwordEncoder.encode(req.getPassword()));

        return toDto(userRepository.save(u));
    }

    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO req) {
        if (req.getUsername() == null || req.getUsername().isBlank()) {
            throw new RuntimeException("Username không được để trống");
        }
        if (!SecurityValidator.isValidUsername(req.getUsername())) {
            throw new RuntimeException(SecurityValidator.getInvalidUsernameMessage(req.getUsername()));
        }

        User oldUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // email check nếu đổi email
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (!req.getEmail().equalsIgnoreCase(oldUser.getEmail()) && userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // username check nếu đổi username
        if (!req.getUsername().equalsIgnoreCase(oldUser.getUsername()) && userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        oldUser.setUsername(req.getUsername().trim());
        oldUser.setEmail(req.getEmail().trim());
        oldUser.setPhone(req.getPhone());
        oldUser.setRole(req.getRole() == null ? oldUser.getRole() : req.getRole());

        // update password nếu nhập
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            oldUser.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        return toDto(userRepository.save(oldUser));
    }
    @Override
    public User register(User user) throws Exception {
        // Check email exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new Exception("Email đã tồn tại");
        }

        // Check username exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new Exception("Username đã tồn tại");
        }

        // Validate username
        if (!SecurityValidator.isValidUsername(user.getUsername())) {
            throw new Exception(SecurityValidator.getInvalidUsernameMessage(user.getUsername()));
        }

        // Require password
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new Exception("Mật khẩu không được để trống");
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null) {
            user.setRole(User.Role.CUSTOMER);
        }

        return userRepository.save(user);
    }
    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public StaffModulesDTO getStaffModules(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        StaffModulesDTO dto = new StaffModulesDTO();
        dto.setModules(splitModules(u.getStaffModules()));
        return dto;
    }

    @Override
    public StaffModulesDTO updateStaffModules(Long userId, StaffModulesDTO dto) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        if (u.getRole() != User.Role.STAFF) {
            throw new RuntimeException("Chỉ cấp modules cho nhân viên (STAFF)");
        }

        String normalized = normalizeModules(dto.getModules());
        u.setStaffModules(normalized);
        userRepository.save(u);

        StaffModulesDTO res = new StaffModulesDTO();
        res.setModules(splitModules(normalized));
        return res;
    }
}