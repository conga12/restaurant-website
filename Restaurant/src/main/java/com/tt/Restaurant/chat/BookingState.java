package com.tt.Restaurant.chat;

public class BookingState {
    public enum Step {
        NONE, ASK_DATE, ASK_TIME, ASK_GUESTS, ASK_NAME, ASK_PHONE, ASK_EMAIL, ASK_NOTE, CONFIRM
    }

    private Step step = Step.NONE;

    private String date;   // YYYY-MM-DD
    private String time;   // HH:mm
    private Integer guests;
    private String name;
    private String phone;
    private String email;
    private String note;

    public Step getStep() { return step; }
    public void setStep(Step step) { this.step = step; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}