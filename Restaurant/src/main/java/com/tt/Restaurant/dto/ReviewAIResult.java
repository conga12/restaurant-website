package com.tt.Restaurant.dto;

import java.util.List;

public class ReviewAIResult {
    private String sentiment;      // POSITIVE/NEGATIVE/NEUTRAL
    private String summary;        // tóm tắt <= 25 từ
    private String ownerReply;     // phản hồi công khai (không nhắc coupon)

    // ===== NEW =====
    private Boolean shouldBlock;   // true => chặn review
    private String blockReason;    // PROFANITY/SEXUAL/HATE/OTHER...
    private Boolean negative;      // tiêu cực dựa trên nội dung (dù 5 sao)
    private String severity;       // LOW/MEDIUM/HIGH
    private String emailBody;      // email xin lỗi + coupon (khi cần)
    private List<String> profanityWords; // optional

    public ReviewAIResult() {}

    public ReviewAIResult(String sentiment, String summary, String ownerReply) {
        this.sentiment = sentiment;
        this.summary = summary;
        this.ownerReply = ownerReply;
    }

    public String getSentiment() { return sentiment; }
    public String getSummary() { return summary; }
    public String getOwnerReply() { return ownerReply; }
    public Boolean getShouldBlock() { return shouldBlock; }
    public String getBlockReason() { return blockReason; }
    public Boolean getNegative() { return negative; }
    public String getSeverity() { return severity; }
    public String getEmailBody() { return emailBody; }
    public List<String> getProfanityWords() { return profanityWords; }

    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setOwnerReply(String ownerReply) { this.ownerReply = ownerReply; }
    public void setShouldBlock(Boolean shouldBlock) { this.shouldBlock = shouldBlock; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    public void setNegative(Boolean negative) { this.negative = negative; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setEmailBody(String emailBody) { this.emailBody = emailBody; }
    public void setProfanityWords(List<String> profanityWords) { this.profanityWords = profanityWords; }
}