package com.tt.Restaurant.service;

import com.tt.Restaurant.dto.ReviewAIResult;

public interface GeminiService {
    /**
     * Phân tích cảm xúc của một đoạn văn bản đánh giá.
     * @param comment Nội dung đánh giá của khách
     * @return POSITIVE, NEGATIVE, hoặc NEUTRAL
     */
    String analyzeSentiment(String comment);
    ReviewAIResult analyzeReview(String comment, Integer rating);
}