package com.tt.Restaurant.service.impl;

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
}