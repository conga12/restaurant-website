package com.tt.Restaurant.repository;

import com.tt.Restaurant.model.OrderAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderAccessTokenRepository extends JpaRepository<OrderAccessToken, Long> {
    Optional<OrderAccessToken> findByToken(String token);
}