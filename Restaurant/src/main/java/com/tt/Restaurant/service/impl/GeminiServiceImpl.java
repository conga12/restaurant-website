package com.tt.Restaurant.service.impl;

import com.tt.Restaurant.dto.ReviewAIResult;
import com.tt.Restaurant.service.GeminiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Override
    public String analyzeSentiment(String comment) {
        // 1. Tạo Prompt ép AI trả về đúng định dạng
        String prompt = "Đóng vai một chuyên gia phân tích cảm xúc khách hàng nhà hàng. " +
                "Hãy đọc đánh giá sau và chỉ trả về đúng 1 từ tiếng Anh đại diện cho cảm xúc: " +
                "POSITIVE (Tích cực), NEGATIVE (Tiêu cực), hoặc NEUTRAL (Trung lập). " +
                "Không giải thích thêm. Đánh giá: \"" + comment + "\"";

        // 2. Format Body JSON. Thay vì replace thủ công, dùng replaceAll cẩn thận hơn để tránh lỗi chuỗi
        String safeComment = prompt.replace("\"", "\\\"").replace("\n", " ");
        String requestBody = String.format(
                "{ \"contents\": [{ \"parts\": [{ \"text\": \"%s\" }] }] }",
                safeComment
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 3. Bắn request sang Google Gemini
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // 4. Bóc tách JSON lấy kết quả
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.getBody());
            String sentiment = rootNode.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText().trim();

            return sentiment.toUpperCase();

        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Gemini: " + e.getMessage());
            // Trả về mặc định nếu API lỗi (hết quota, rớt mạng...) để không làm chết luồng chính
            return "NEUTRAL";
        }
    }

    @Override
    public ReviewAIResult analyzeReview(String comment, Integer rating) {
        try {
            String prompt = """
                Bạn là AI hỗ trợ nhà hàng Hương Việt.

                Hãy phân tích review sau và trả về đúng 3 dòng, không thêm giải thích:
                SENTIMENT: POSITIVE hoặc NEGATIVE hoặc NEUTRAL
                SUMMARY: tóm tắt ngắn bằng tiếng Việt, tối đa 25 từ
                REPLY: phản hồi lịch sự từ nhà hàng, tối đa 45 từ

                Rating: %s/5
                Review: %s
                """.formatted(rating, comment);

            String aiText = generateContent(prompt);

            String sentiment = extractLine(aiText, "SENTIMENT:", "NEUTRAL");
            String summary = extractLine(aiText, "SUMMARY:", comment);
            String reply = extractLine(aiText, "REPLY:", "Cảm ơn bạn đã chia sẻ trải nghiệm. Nhà hàng sẽ ghi nhận và cải thiện dịch vụ tốt hơn.");

            sentiment = sentiment.toUpperCase();
            if (!sentiment.equals("POSITIVE") && !sentiment.equals("NEGATIVE") && !sentiment.equals("NEUTRAL")) {
                sentiment = "NEUTRAL";
            }

            return new ReviewAIResult(sentiment, summary, reply);
        } catch (Exception e) {
            return new ReviewAIResult(
                    "NEUTRAL",
                    comment,
                    "Cảm ơn bạn đã chia sẻ trải nghiệm. Nhà hàng sẽ ghi nhận góp ý của bạn."
            );
        }
    }

    private String extractLine(String text, String prefix, String fallback) {
        if (text == null || text.isBlank()) return fallback;

        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase().startsWith(prefix.toUpperCase())) {
                return trimmed.substring(prefix.length()).trim();
            }
        }

        return fallback;
    }
    private String generateContent(String prompt) {
        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", " ");

        String requestBody = String.format(
                "{ \"contents\": [{ \"parts\": [{ \"text\": \"%s\" }] }] }",
                safePrompt
        );

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.getBody());

            return rootNode.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText()
                    .trim();

        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Gemini generateContent: " + e.getMessage());
            return "";
        }
    }
}