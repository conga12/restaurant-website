package com.tt.Restaurant.dto;

public class AIChatResponse {
    private String reply;
    private boolean done;
    private String nextField;
    private String reservationId;

    public AIChatResponse() {}

    public AIChatResponse(String reply) {
        this.reply = reply;
    }

    public AIChatResponse(String reply, boolean done, String nextField, String reservationId) {
        this.reply = reply;
        this.done = done;
        this.nextField = nextField;
        this.reservationId = reservationId;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getNextField() { return nextField; }
    public void setNextField(String nextField) { this.nextField = nextField; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
}