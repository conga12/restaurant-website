package com.tt.Restaurant.controller;

import com.tt.Restaurant.dto.StaffModulesDTO;
import com.tt.Restaurant.dto.UserRequestDTO;
import com.tt.Restaurant.dto.UserResponseDTO;
import com.tt.Restaurant.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDTO getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public UserResponseDTO createUser(@RequestBody UserRequestDTO req) {
        return userService.createUser(req);
    }

    @PutMapping("/{id}")
    public UserResponseDTO updateUser(@PathVariable Long id, @RequestBody UserRequestDTO req) {
        return userService.updateUser(id, req);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @GetMapping("/{id}/modules")
    public StaffModulesDTO getStaffModules(@PathVariable Long id) {
        return userService.getStaffModules(id);
    }

    @PutMapping("/{id}/modules")
    public StaffModulesDTO updateStaffModules(@PathVariable Long id, @RequestBody StaffModulesDTO dto) {
        return userService.updateStaffModules(id, dto);
    }
}