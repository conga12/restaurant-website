package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.model.OrderAccessToken;
import com.tt.Restaurant.repository.OrderAccessTokenRepository;
import com.tt.Restaurant.service.OrderMagicLinkService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class OrderMagicLinkServiceImpl implements OrderMagicLinkService {

    private final OrderAccessTokenRepository repo;
    private final SecureRandom random = new SecureRandom();

    public OrderMagicLinkServiceImpl(OrderAccessTokenRepository repo) {
        this.repo = repo;
    }

    @Override
    public OrderAccessToken createToken(Long reservationId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        OrderAccessToken t = new OrderAccessToken();
        t.setToken(token);
        t.setReservationId(reservationId);
        t.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        t.setRevoked(false);

        return repo.save(t);
    }

    @Override
    public OrderAccessToken validateToken(String token) {
        OrderAccessToken t = repo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link không hợp lệ"));

        if (Boolean.TRUE.equals(t.getRevoked())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link đã bị thu hồi");
        }

        if (t.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link đã hết hạn");
        }

        return t;
    }
}