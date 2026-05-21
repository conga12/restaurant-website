package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.ReviewAIResult;
import com.tt.Restaurant.service.GeminiService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Simple mock fallback for Gemini — safe defaults, never blocks.
 * Enabled by property gemini.mock.enabled=true (default true if missing).
 */
@Service
@ConditionalOnProperty(name = "gemini.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockGeminiService implements GeminiService {

    @Override
    public String analyzeSentiment(String comment) {
        return "NEUTRAL";
    }

    @Override
    public ReviewAIResult analyzeReview(String comment, Integer rating) {
        ReviewAIResult r = new ReviewAIResult();
        r.setSentiment(rating != null && rating >= 4 ? "POSITIVE"
                : rating != null && rating <= 2 ? "NEGATIVE" : "NEUTRAL");
        r.setSummary("Tóm tắt (mock)");
        r.setOwnerReply("Cảm ơn bạn đã phản hồi. Nhà hàng sẽ ghi nhận và cải thiện.");
        r.setShouldBlock(false);
        return r;
    }

    @Override
    public ReviewAIResult analyzeAndModerateReview(String comment, Integer rating) {
        return analyzeReview(comment, rating);
    }

    @Override
    public ReviewAIResult generateLowRatingReplyEmail(String customerName, Integer rating,
                                                      String customerComment, String couponCode,
                                                      int discountPercent, String expiresAtText) {
        ReviewAIResult r = new ReviewAIResult();
        r.setOwnerReply("Xin lỗi " + (customerName == null ? "khách" : customerName) + ". Mã: " + couponCode);
        r.setShouldBlock(false);
        return r;
    }

    @Override
    public ReviewAIResult moderateReviewImage(byte[] imageBytes, String mimeType) {
        ReviewAIResult r = new ReviewAIResult();
        r.setShouldBlock(false); // fallback: do NOT block on error
        return r;
    }

    @Override
    public String chatSupport(String userMessage) {
        return "Xin chào! Đây là phản hồi mock cho demo.";
    }
}