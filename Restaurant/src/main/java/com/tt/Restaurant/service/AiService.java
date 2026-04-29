package com.tt.Restaurant.service;

import com.tt.Restaurant.dto.AiApologyResponse;

public interface AiService {
    AiApologyResponse generateLowRatingReply(
            String customerName,
            int rating,
            String customerComment,
            String couponCode,
            int discountPercent,
            String expiresAtText
    );
}