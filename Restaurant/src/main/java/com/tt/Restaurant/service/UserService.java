package com.tt.Restaurant.service;

import com.tt.Restaurant.dto.StaffModulesDTO;
import com.tt.Restaurant.dto.UserRequestDTO;
import com.tt.Restaurant.dto.UserResponseDTO;
import com.tt.Restaurant.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserResponseDTO> getAllUsers();
    UserResponseDTO getUserById(Long id);
    UserResponseDTO createUser(UserRequestDTO req);
    UserResponseDTO updateUser(Long id, UserRequestDTO req);
    User register(User user) throws Exception;
    void deleteUser(Long id);

    // nếu bạn vẫn cần register kiểu cũ thì giữ lại, nhưng nên chuyển qua DTO sau
    // User register(User user) throws Exception;

    Optional<com.tt.Restaurant.model.User> findByEmail(String email);
    Optional<com.tt.Restaurant.model.User> findByUsername(String username);
    StaffModulesDTO getStaffModules(Long userId);
    StaffModulesDTO updateStaffModules(Long userId, StaffModulesDTO dto);
}