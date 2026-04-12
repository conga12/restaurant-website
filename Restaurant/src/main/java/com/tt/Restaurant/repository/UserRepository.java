package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ← THÊM: Tìm user by email
    Optional<User> findByEmail(String email);

    // ← THÊM: Tìm user by username
    Optional<User> findByUsername(String username);

    // ← THÊM: Check email tồn tại
    boolean existsByEmail(String email);

    // ← THÊM: Check username tồn tại
    boolean existsByUsername(String username);
}