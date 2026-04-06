package com.tt.Restaurant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String userMessage) {
        try {
            String url = "https://api.openai.com/v1/responses";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", "Bạn là chatbot hỗ trợ nhà hàng. "
                    + "Trả lời ngắn gọn, lịch sự, bằng tiếng Việt. "
                    + "Chỉ hỗ trợ các chủ đề như đặt bàn, menu, giờ mở cửa, địa chỉ, liên hệ. "
                    + "Nếu người dùng hỏi ngoài phạm vi nhà hàng thì trả lời ngắn và hướng họ quay lại chủ đề.\n\n"
                    + "Khách: " + userMessage);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return "Xin lỗi, tôi đang bận. Bạn thử lại sau nhé.";
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode output = root.path("output");

            if (output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode content = item.path("content");
                    if (content.isArray()) {
                        for (JsonNode c : content) {
                            if ("output_text".equals(c.path("type").asText())) {
                                return c.path("text").asText("Xin lỗi, tôi chưa có câu trả lời phù hợp.");
                            }
                        }
                    }
                }
            }

            return "Xin lỗi, tôi chưa có câu trả lời phù hợp.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hệ thống AI đang gặp lỗi.";
        }
    }
}