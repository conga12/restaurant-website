package com.tt.Restaurant.chat;

import java.time.LocalDate;
import java.time.LocalTime;

public class NaturalLanguageSlot {
    private LocalDate date;
    private LocalTime time;
    private Integer guests;
    private String original;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }

    public String getOriginal() { return original; }
    public void setOriginal(String original) { this.original = original; }
}