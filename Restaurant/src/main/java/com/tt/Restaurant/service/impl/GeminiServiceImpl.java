package com.tt.Restaurant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tt.Restaurant.dto.ReviewAIResult;
import com.tt.Restaurant.service.GeminiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Locale;
import java.util.Base64;

@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.text.url}")
    private String textUrl;

    @Value("${gemini.vision.url}")
    private String visionUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String analyzeSentiment(String comment) {
        String prompt = "Đóng vai một chuyên gia phân tích cảm xúc khách hàng nhà hàng. " +
                "Hãy đọc đánh giá sau và chỉ trả về đúng 1 từ tiếng Anh đại diện cho cảm xúc: " +
                "POSITIVE (Tích cực), NEGATIVE (Tiêu cực), hoặc NEUTRAL (Trung lập). " +
                "Không giải thích thêm. Đánh giá: \"" + comment + "\"";

        String aiText = generateContent(prompt);
        if (aiText == null) return "NEUTRAL";
        String sentiment = aiText.trim().toUpperCase(Locale.ROOT);
        if (!sentiment.equals("POSITIVE") && !sentiment.equals("NEGATIVE") && !sentiment.equals("NEUTRAL")) {
            return "NEUTRAL";
        }
        return sentiment;
    }

    @Override
    public ReviewAIResult analyzeReview(String comment, Integer rating) {
        // giữ lại method cũ để không vỡ chỗ khác đang gọi
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

            sentiment = sentiment.toUpperCase(Locale.ROOT);
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

    // ===== NEW =====
    @Override
    public ReviewAIResult analyzeAndModerateReview(String comment, Integer rating) {
        String safeComment = comment == null ? "" : comment.trim();

        // Nếu comment trống mà vẫn cho review -> bạn có thể quyết định block hoặc allow.
        // Ở đây: allow (không block) và để AI xử lý bình thường.
        String prompt = """
        Bạn là hệ thống KIỂM DUYỆT + PHÂN TÍCH review cho nhà hàng Hương Việt.
        Bạn phải tuân thủ tuyệt đối định dạng output.
        
        Mục tiêu:
        A) MODERATION:
        - Nếu review có ngôn từ thô tục/xúc phạm/chửi bới, kích động thù ghét, nội dung tình dục, quấy rối, miệt thị, hoặc nội dung không phù hợp => shouldBlock=true.
        - Nếu shouldBlock=true: ownerReply phải để rỗng "".
        
        B) PHÂN TÍCH (CHỈ khi shouldBlock=false):
        - sentiment ∈ {POSITIVE, NEUTRAL, NEGATIVE}
        - negative=true/false: đánh giá "tiêu cực theo nội dung" KHÔNG phụ thuộc số sao.
          Ví dụ:
          - Rating 5/5 nhưng review: "đồ ăn tệ, phục vụ chậm, không quay lại, kém" => sentiment=NEGATIVE, negative=true, severity=HIGH
          - Rating 5/5 nhưng review: "ngon nhưng chờ lâu, giá món ăn hơi đắt" => sentiment=NEUTRAL hoặc NEGATIVE, negative=true, severity=MEDIUM
          - Rating 5/5 nhưng review: "rất ngon, phục vụ tuyệt vời" => sentiment=POSITIVE, negative=false, severity=LOW
        
        C) TÓM TẮT:
        - summary: tiếng Việt, tối đa 25 từ.
        
        D) PHẢN HỒI CÔNG KHAI:
        - ownerReply: 1-2 câu, lịch sự, KHÔNG nhắc coupon/mã giảm giá.
        - Nếu negative=true: xin lỗi + ghi nhận + mời góp ý thêm (ngắn gọn).
        
        OUTPUT:
        - Chỉ được trả về DUY NHẤT 1 JSON object hợp lệ, MỘT DÒNG DUY NHẤT (single-line).
        - Không được bọc ``` hoặc markdown.
        Schema chính xác:
        {"shouldBlock":false,"blockReason":"","profanityWords":[],"sentiment":"NEUTRAL","negative":false,"severity":"LOW","summary":"","ownerReply":""}
        
        INPUT:
        Rating: %s/5
        Review: %s
        """.formatted(rating == null ? "?" : rating, safeComment);

        String aiText = generateContent(prompt);

        // ===== AI-first + fail-safe =====
        // AI trả rỗng => block (vì bạn muốn AI làm cơ và muốn chặn chắc)
        if (aiText == null || aiText.isBlank()) {
            ReviewAIResult fb = new ReviewAIResult();
            fb.setShouldBlock(true);
            fb.setBlockReason("AI_EMPTY_RESPONSE");
            fb.setProfanityWords(java.util.List.of());
            fb.setSentiment("NEUTRAL");
            fb.setNegative(true);
            fb.setSeverity("HIGH");
            fb.setSummary("Nội dung cần kiểm duyệt thủ công.");
            fb.setOwnerReply("");
            return fb;
        }

        String json = normalizeJson(aiText);

        try {
            ReviewAIResult parsed = objectMapper.readValue(json, ReviewAIResult.class);

            // ===== normalize fields =====
            if (parsed.getShouldBlock() == null) parsed.setShouldBlock(true); // nếu thiếu field => block
            if (parsed.getBlockReason() == null) parsed.setBlockReason(parsed.getShouldBlock() ? "AI_MISSING_BLOCK_REASON" : "");
            if (parsed.getProfanityWords() == null) parsed.setProfanityWords(java.util.List.of());

            String s = parsed.getSentiment() == null ? "NEUTRAL" : parsed.getSentiment().trim().toUpperCase(Locale.ROOT);
            if (!s.equals("POSITIVE") && !s.equals("NEGATIVE") && !s.equals("NEUTRAL")) s = "NEUTRAL";
            parsed.setSentiment(s);

            // severity default
            if (parsed.getSeverity() == null || parsed.getSeverity().isBlank()) parsed.setSeverity("LOW");
            String sev = parsed.getSeverity().trim().toUpperCase(Locale.ROOT);
            if (!sev.equals("LOW") && !sev.equals("MEDIUM") && !sev.equals("HIGH")) sev = "LOW";
            parsed.setSeverity(sev);

            // negative default: nếu AI không set thì suy ra từ sentiment
            if (parsed.getNegative() == null) {
                parsed.setNegative("NEGATIVE".equals(parsed.getSentiment()));
            }

            // summary fallback
            if (parsed.getSummary() == null || parsed.getSummary().isBlank()) parsed.setSummary(safeComment);

            // Nếu shouldBlock=true => ownerReply phải rỗng
            if (Boolean.TRUE.equals(parsed.getShouldBlock())) {
                parsed.setOwnerReply("");
                if (parsed.getBlockReason() == null || parsed.getBlockReason().isBlank()) {
                    parsed.setBlockReason("CONTENT_POLICY");
                }
                return parsed;
            }

            // shouldBlock=false => ownerReply fallback
            if (parsed.getOwnerReply() == null || parsed.getOwnerReply().isBlank()) {
                if (Boolean.TRUE.equals(parsed.getNegative())) {
                    parsed.setOwnerReply("Nhà hàng rất tiếc vì trải nghiệm của bạn chưa tốt. Chúng tôi đã ghi nhận góp ý và sẽ cải thiện sớm nhất.");
                } else {
                    parsed.setOwnerReply("Cảm ơn bạn đã chia sẻ trải nghiệm. Nhà hàng sẽ tiếp tục cải thiện để phục vụ tốt hơn.");
                }
            }

            return parsed;
        } catch (Exception e) {
            // Parse fail => block (AI-first + fail-safe)
            ReviewAIResult fb = new ReviewAIResult();
            fb.setShouldBlock(true);
            fb.setBlockReason("AI_JSON_PARSE_ERROR");
            fb.setProfanityWords(java.util.List.of());
            fb.setSentiment("NEUTRAL");
            fb.setNegative(true);
            fb.setSeverity("HIGH");
            fb.setSummary("Nội dung cần kiểm duyệt thủ công.");
            fb.setOwnerReply("");
            return fb;
        }
    }
    @Override
    public ReviewAIResult moderateReviewImage(byte[] imageBytes, String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            ReviewAIResult r = new ReviewAIResult();
            r.setShouldBlock(true);
            r.setBlockReason("MISSING_MIMETYPE");
            return r;
        }
        if (imageBytes == null || imageBytes.length == 0) {
            ReviewAIResult r = new ReviewAIResult();
            r.setShouldBlock(true);
            r.setBlockReason("EMPTY_IMAGE");
            return r;
        }

        String b64 = Base64.getEncoder().encodeToString(imageBytes);

        String prompt = """
        Bạn là hệ thống kiểm duyệt ảnh cho review nhà hàng.
        Nếu ảnh chứa nội dung khiêu dâm/tình dục, thô tục, bạo lực cực đoan, ghê rợn, hoặc phản cảm => shouldBlock=true.
        Trả về DUY NHẤT JSON hợp lệ, không markdown, không ```:
        {"shouldBlock":false,"blockReason":"","summary":""}
        """;

                // camelCase inlineData
                String requestBody = """
        {
          "contents": [{
            "parts": [
              { "text": %s },
              {
                "inlineData": {
                  "mimeType": %s,
                  "data": %s
                }
              }
            ]
          }]
        }
        """.formatted(
                objectMapper.valueToTree(prompt).toString(),
                objectMapper.valueToTree(mimeType).toString(),
                objectMapper.valueToTree(b64).toString()
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    visionUrl + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode candidates = rootNode.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                ReviewAIResult r = new ReviewAIResult();
                r.setShouldBlock(true);
                r.setBlockReason("NO_CANDIDATES");
                return r;
            }

            String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText("").trim();
            String json = normalizeJson(text);

            ReviewAIResult parsed = objectMapper.readValue(json, ReviewAIResult.class);
            if (parsed.getShouldBlock() == null) parsed.setShouldBlock(false);
            if (parsed.getBlockReason() == null) parsed.setBlockReason("");
            return parsed;

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Gemini(VISION) HTTP error: " + e.getStatusCode());
            System.err.println("Gemini(VISION) error body: " + e.getResponseBodyAsString());
            ReviewAIResult r = new ReviewAIResult();
            r.setShouldBlock(true);
            r.setBlockReason("IMAGE_MODERATION_FAILED");
            return r;
        } catch (Exception e) {
            System.err.println("Gemini(VISION) exception: " + e);
            ReviewAIResult r = new ReviewAIResult();
            r.setShouldBlock(true);
            r.setBlockReason("IMAGE_MODERATION_FAILED");
            return r;
        }
    }
    // ===== NEW =====
    @Override
    public ReviewAIResult generateLowRatingReplyEmail(
            String customerName,
            Integer rating,
            String customerComment,
            String couponCode,
            int discountPercent,
            String expiresAtText
    ) {
        String safeName = (customerName == null || customerName.isBlank()) ? "bạn" : customerName.trim();
        String safeComment = customerComment == null ? "" : customerComment.trim();

        String prompt = """
        Bạn là trợ lý CSKH nhà hàng Hương Việt. Viết tiếng Việt, lịch sự, chân thành.
        Quy tắc:
        - Không cam kết hoàn tiền/đền bù ngoài mã giảm giá đã cấp.
        - Không thừa nhận lỗi pháp lý.
        - emailBody: email xin lỗi ngắn gọn, có hướng dẫn dùng mã giảm giá.
        - Trả về DUY NHẤT JSON hợp lệ, không markdown, không ```:
        {
          "emailBody": "..."
        }
        
        Thông tin:
        - Tên khách: %s
        - Rating: %s/5
        - Nội dung khách: %s
        - Coupon: %s
        - Giảm: %d%%
        - Hạn dùng: %s
        """.formatted(
                safeName,
                rating == null ? "?" : rating,
                safeComment,
                couponCode,
                discountPercent,
                expiresAtText
        );

        String aiText = generateContent(prompt);
        String json = normalizeJson(aiText);

        try {
            ReviewAIResult parsed = objectMapper.readValue(json, ReviewAIResult.class);
            if (parsed.getEmailBody() == null || parsed.getEmailBody().isBlank()) {
                parsed.setEmailBody(buildLowRatingEmailFallback(safeName, rating, couponCode, discountPercent, expiresAtText));
            }
            return parsed;
        } catch (Exception e) {
            ReviewAIResult fb = new ReviewAIResult();
            fb.setEmailBody(buildLowRatingEmailFallback(safeName, rating, couponCode, discountPercent, expiresAtText));
            return fb;
        }
    }

    @Override
    public String chatSupport(String userMessage) {
        String msg = userMessage == null ? "" : userMessage.trim();
        if (msg.isBlank()) return "Bạn vui lòng nhập nội dung cần hỗ trợ.";

        String prompt = """
        Bạn là chatbot CSKH của nhà hàng Hương Việt. Trả lời tiếng Việt, ngắn gọn, lịch sự (tối đa 4-6 câu).
        Nếu nội dung có chửi bậy/xúc phạm/tình dục/thù ghét: trả đúng 1 câu:
        "Xin lỗi, mình không thể hỗ trợ nội dung này. Bạn vui lòng dùng ngôn từ phù hợp."
        
        Nếu khách hỏi:
        - Đặt bàn: hướng dẫn vào mục Reservations/Đặt bàn.
        - Menu: hướng dẫn vào mục Menu.
        - Review: hướng dẫn vào mục Reviews.
        - Liên hệ: hướng dẫn vào mục Contact.
        
        Chỉ trả về plain text, không JSON, không markdown.
        
        Tin nhắn: "%s"
        """.formatted(msg.replace("\"", "'"));

        String ai = generateContent(prompt);
        if (ai == null || ai.isBlank()) {
            return "Xin lỗi, hiện mình chưa thể phản hồi ngay. Bạn vui lòng thử lại sau hoặc xem mục Contact để liên hệ nhà hàng.";
        }
        return ai.trim();
    }

    private String extractLine(String text, String prefix, String fallback) {
        if (text == null || text.isBlank()) return fallback;

        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase(Locale.ROOT).startsWith(prefix.toUpperCase(Locale.ROOT))) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return fallback;
    }

    private String generateContent(String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // Build request body safely (no manual escaping)
            var body = java.util.Map.of(
                    "contents", java.util.List.of(
                            java.util.Map.of(
                                    "parts", java.util.List.of(
                                            java.util.Map.of("text", prompt == null ? "" : prompt)
                                    )
                            )
                    )
            );

            String requestJson = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    visionUrl + apiKey,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getBody() == null || response.getBody().isBlank()) {
                System.err.println("Gemini response body is empty");
                return "";
            }

            JsonNode rootNode = objectMapper.readTree(response.getBody());

            JsonNode candidates = rootNode.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                System.err.println("Gemini returned no candidates. Full response: " + response.getBody());
                return "";
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                System.err.println("Gemini returned no parts. Full response: " + response.getBody());
                return "";
            }

            return parts.get(0).path("text").asText("").trim();

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("Gemini HTTP error: " + e.getStatusCode());
            System.err.println("Gemini error body: " + e.getResponseBodyAsString());
            return "";
        } catch (Exception e) {
            System.err.println("Gemini exception: " + e);
            return "";
        }
    }

    /**
     * Gemini đôi khi trả về ```json ...``` hoặc thêm text thừa.
     * Hàm này cố gắng lấy phần JSON object đầu tiên.
     */
    private String normalizeJson(String aiText) {
        if (aiText == null) return "";
        String t = aiText.trim();

        // strip markdown fences
        t = t.replaceAll("^```json\\s*", "");
        t = t.replaceAll("^```\\s*", "");
        t = t.replaceAll("\\s*```$", "");

        // find first '{' and last '}'
        int first = t.indexOf('{');
        int last = t.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return t.substring(first, last + 1).trim();
        }
        return t;
    }

    private String buildLowRatingEmailFallback(String name, Integer rating, String couponCode, int discountPercent, String expiresAtText) {
        int r = rating == null ? 0 : rating;
        return "Chào " + name + ",\n\n"
                + "Cảm ơn bạn đã phản hồi. Rất tiếc vì trải nghiệm của bạn chưa tốt (" + r + "/5).\n\n"
                + "Để xin lỗi, nhà hàng gửi bạn mã giảm giá " + discountPercent + "% cho order online:\n"
                + "- Mã: " + couponCode + "\n"
                + "- HSD: " + expiresAtText + "\n\n"
                + "Khi thanh toán, bạn nhập mã ở ô 'Mã giảm giá'.\n\n"
                + "Trân trọng,\nHƯƠNG VIỆT";
    }
}