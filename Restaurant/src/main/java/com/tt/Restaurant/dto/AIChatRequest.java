package com.tt.Restaurant.dto;

public class AIChatRequest {
    private String message;
    private String sessionId;

    public AIChatRequest() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}