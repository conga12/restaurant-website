package com.tt.Restaurant.service;

import com.tt.Restaurant.model.OrderAccessToken;

public interface OrderMagicLinkService {
    OrderAccessToken createToken(Long reservationId);
    OrderAccessToken validateToken(String token);
}