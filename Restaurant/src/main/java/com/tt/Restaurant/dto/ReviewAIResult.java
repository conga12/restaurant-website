package com.tt.Restaurant.dto;

public class ReviewAIResult {
    private String sentiment;
    private String summary;
    private String ownerReply;

    public ReviewAIResult() {}

    public ReviewAIResult(String sentiment, String summary, String ownerReply) {
        this.sentiment = sentiment;
        this.summary = summary;
        this.ownerReply = ownerReply;
    }

    public String getSentiment() { return sentiment; }
    public String getSummary() { return summary; }
    public String getOwnerReply() { return ownerReply; }

    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setOwnerReply(String ownerReply) { this.ownerReply = ownerReply; }
}