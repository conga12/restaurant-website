package com.tt.Restaurant.dto;

public class AiApologyResponse {
    private String ownerReply;
    private String emailBody;

    public String getOwnerReply() { return ownerReply; }
    public String getEmailBody() { return emailBody; }

    public void setOwnerReply(String ownerReply) { this.ownerReply = ownerReply; }
    public void setEmailBody(String emailBody) { this.emailBody = emailBody; }
}