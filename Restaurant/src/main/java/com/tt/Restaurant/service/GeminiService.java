package com.tt.Restaurant.service;

public interface GeminiService {
    /**
     * Phân tích cảm xúc của một đoạn văn bản đánh giá.
     * @param comment Nội dung đánh giá của khách
     * @return POSITIVE, NEGATIVE, hoặc NEUTRAL
     */
    String analyzeSentiment(String comment);
}