package com.tt.Restaurant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tt.Restaurant.dto.AiApologyResponse;
import com.tt.Restaurant.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.openai.apiKey:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.openai.enabled:false}")
    private boolean enabled;

    public AiServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .build();
    }

    @Override
    public AiApologyResponse generateLowRatingReply(String customerName, int rating, String customerComment,
                                                    String couponCode, int discountPercent, String expiresAtText) {

        // Fallback không dùng AI
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            AiApologyResponse fallback = new AiApologyResponse();
            fallback.setOwnerReply("Nhà hàng rất tiếc vì trải nghiệm của bạn chưa tốt. Chúng tôi đã ghi nhận góp ý và s�� cải thiện trong thời gian sớm nhất.");
            fallback.setEmailBody(
                    "Cảm ơn bạn đã phản hồi. Rất tiếc vì trải nghiệm chưa tốt.\n\n" +
                            "Để xin lỗi, nhà hàng gửi bạn mã giảm giá " + discountPercent + "% cho order online:\n" +
                            "Mã: " + couponCode + "\n" +
                            "HSD: " + expiresAtText + "\n\n" +
                            "Khi thanh toán, bạn nhập mã ở ô 'Mã giảm giá'.\n\n" +
                            "Trân trọng,\nHƯƠNG VIỆT"
            );
            return fallback;
        }

        // System policy: không hứa bừa, ownerReply không lộ coupon
        String system = """
Bạn là trợ lý CSKH cho nhà hàng. Viết tiếng Việt, lịch sự, chân thành.
Quy tắc:
- Không cam kết hoàn tiền/đền bù ngoài mã khuyến mãi đã cấp.
- Không xin lỗi theo kiểu thừa nhận lỗi pháp lý.
- ownerReply: 1-2 câu, công khai, KHÔNG nhắc coupon/mã.
- emailBody: email xin lỗi, ngắn gọn, có hướng dẫn dùng mã giảm giá.
- Trả về DUY NHẤT JSON hợp lệ: {"ownerReply":"...","emailBody":"..."}.
""";

        String user = """
Thông tin:
- Tên khách: %s
- Rating: %d/5
- Nội dung khách: %s
- Coupon: %s
- Giảm: %d%%
- Hạn dùng: %s
Hãy tạo ownerReply và emailBody theo quy tắc.
""".formatted(
                customerName == null ? "Khách hàng" : customerName,
                rating,
                customerComment == null ? "" : customerComment,
                couponCode,
                discountPercent,
                expiresAtText
        );

        // Gọi Chat Completions kiểu đơn giản (tùy bạn đang dùng endpoint nào)
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", new Object[] {
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                },
                "temperature", 0.5
        );

        ResponseEntity<String> resp = restClient.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        try {
            // Parse: choices[0].message.content là JSON string
            Map<?, ?> json = objectMapper.readValue(resp.getBody(), Map.class);
            var choices = (java.util.List<?>) json.get("choices");
            var choice0 = (Map<?, ?>) choices.get(0);
            var message = (Map<?, ?>) choice0.get("message");
            String content = (String) message.get("content");

            return objectMapper.readValue(content, AiApologyResponse.class);
        } catch (Exception e) {
            // fallback nếu AI trả về sai format
            AiApologyResponse fallback = new AiApologyResponse();
            fallback.setOwnerReply("Nhà hàng rất tiếc vì trải nghiệm của bạn chưa tốt. Chúng tôi đã ghi nhận góp ý và sẽ cải thiện.");
            fallback.setEmailBody(
                    "Rất tiếc vì trải nghiệm của bạn chưa tốt.\n\n" +
                            "Mã giảm giá " + discountPercent + "%: " + couponCode + "\n" +
                            "HSD: " + expiresAtText + "\n\n" +
                            "Trân trọng,\nHƯƠNG VIỆT"
            );
            return fallback;
        }
    }
}