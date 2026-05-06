package com.tt.Restaurant.service;

import com.tt.Restaurant.dto.ReviewAIResult;

public interface GeminiService {
    String analyzeSentiment(String comment);

    ReviewAIResult analyzeReview(String comment, Integer rating);

    ReviewAIResult analyzeAndModerateReview(String comment, Integer rating);

    ReviewAIResult generateLowRatingReplyEmail(
            String customerName,
            Integer rating,
            String customerComment,
            String couponCode,
            int discountPercent,
            String expiresAtText
    );

    // NEW: kiểm duyệt ảnh review (NSFW/ phản cảm) => shouldBlock=true nếu vi phạm
    ReviewAIResult moderateReviewImage(byte[] imageBytes, String mimeType);

    String chatSupport(String userMessage);
}