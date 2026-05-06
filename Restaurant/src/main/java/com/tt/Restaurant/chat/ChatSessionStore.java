package com.tt.Restaurant.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatSessionStore {
    private final Map<String, BookingState> bookingSessions = new ConcurrentHashMap<>();

    public BookingState getOrCreate(String sessionId) {
        return bookingSessions.computeIfAbsent(sessionId, k -> new BookingState());
    }

    public void clear(String sessionId) {
        bookingSessions.remove(sessionId);
    }
}