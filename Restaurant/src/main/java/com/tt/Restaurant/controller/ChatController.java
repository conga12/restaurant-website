package com.tt.Restaurant.controller;

import com.tt.Restaurant.chat.BookingState;
import com.tt.Restaurant.chat.ChatSessionStore;
import com.tt.Restaurant.dto.AIChatRequest;
import com.tt.Restaurant.dto.AIChatResponse;
import com.tt.Restaurant.service.GeminiService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.tt.Restaurant.dto.AvailableTableDTO;
import com.tt.Restaurant.service.TableAvailabilityService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GeminiService geminiService;
    private final TableAvailabilityService tableAvailabilityService;
    private final ChatSessionStore chatSessionStore;

    public ChatController(GeminiService geminiService,
                          TableAvailabilityService tableAvailabilityService,
                          ChatSessionStore chatSessionStore) {
        this.geminiService = geminiService;
        this.tableAvailabilityService = tableAvailabilityService;
        this.chatSessionStore = chatSessionStore;
    }

    @PostMapping("/send")
    public AIChatResponse send(@RequestBody AIChatRequest req) {
        String msg = req == null ? "" : (req.getMessage() == null ? "" : req.getMessage().trim());
        if (msg.isBlank()) {
            return new AIChatResponse("Bạn vui lòng nhập nội dung cần hỗ trợ.", true, "", "");
        }

        String sessionId = (req.getSessionId() == null || req.getSessionId().isBlank())
                ? UUID.randomUUID().toString()
                : req.getSessionId().trim();

        // lệnh hủy
        if (isCancel(msg)) {
            chatSessionStore.clear(sessionId);
            return new AIChatResponse("Đã hủy tiến trình đặt bàn. Bạn cần mình hỗ trợ gì nữa không?", true, "", "");
        }

        BookingState state = chatSessionStore.getOrCreate(sessionId);

        // intent đặt bàn
        if (state.getStep() == BookingState.Step.NONE && isBookingIntent(msg)) {
            state.setStep(BookingState.Step.ASK_DATE);
            return new AIChatResponse("Ok bạn. Bạn muốn đặt bàn ngày nào? (VD: 2026-05-10)", false, "date", "");
        }

        // đang trong flow
        if (state.getStep() != BookingState.Step.NONE) {
            return handleBookingFlow(state, sessionId, msg);
        }

        // fallback Q&A
        String reply = geminiService.chatSupport(msg);
        return new AIChatResponse(reply, true, "", "");
    }

    private AIChatResponse handleBookingFlow(BookingState state, String sessionId, String msg) {
        String text = msg.trim();

        switch (state.getStep()) {
            case ASK_DATE -> {
                String normalized = text.toLowerCase().trim();
                String dateStr = null;

                if (normalized.contains("hôm nay") || normalized.equals("hn")) {
                    dateStr = LocalDate.now().toString();
                } else if (normalized.contains("ngày mai") || normalized.equals("mai") || normalized.contains("mai")) {
                    dateStr = LocalDate.now().plusDays(1).toString();
                } else if (normalized.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    dateStr = normalized;
                }

                if (dateStr == null) {
                    return new AIChatResponse(
                            "Bạn muốn kiểm tra bàn trống ngày nào? (gõ 'hôm nay', 'ngày mai' hoặc YYYY-MM-DD, ví dụ 2026-05-10)",
                            false, "date", ""
                    );
                }

                // validate thật
                try {
                    LocalDate.parse(dateStr);
                } catch (Exception e) {
                    return new AIChatResponse("Ngày không hợp lệ. Bạn nhập lại giúp mình nhé.", false, "date", "");
                }

                state.setDate(dateStr);
                state.setStep(BookingState.Step.ASK_GUESTS);
                return new AIChatResponse("Bạn đi mấy người?", false, "guests", "");
            }

            case ASK_GUESTS -> {
                Integer guests = parseIntSafe(text);
                if (guests == null || guests < 1 || guests > 50) {
                    return new AIChatResponse("Số người không hợp lệ (1–50). Bạn nhập lại nhé.", false, "guests", "");
                }
                state.setGuests(guests);

                LocalDate date = LocalDate.parse(state.getDate());

                List<LocalTime> slots = List.of(
                        LocalTime.of(17, 0),
                        LocalTime.of(18, 0),
                        LocalTime.of(19, 0),
                        LocalTime.of(20, 0),
                        LocalTime.of(21, 0)
                );

                var availability = tableAvailabilityService.findAvailableTablesByDay(date, guests, slots);

                String reply = buildAvailabilityByDayReply(state.getDate(), guests, availability);

                chatSessionStore.clear(sessionId);
                return new AIChatResponse(reply, true, "", "");
            }

            default -> {
                chatSessionStore.clear(sessionId);
                return new AIChatResponse("Mình bị lỗi luồng. Bạn gõ 'bàn trống' để bắt đầu lại nhé.", true, "", "");
            }
        }
    }
    private String buildAvailabilityByDayReply(
            String date,
            int guests,
            java.util.Map<LocalTime, List<AvailableTableDTO>> availability
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kết quả bàn trống ngày ").append(date)
                .append(" cho ").append(guests).append(" người:\n");

        boolean any = false;

        for (var entry : availability.entrySet()) {
            LocalTime slot = entry.getKey();
            List<AvailableTableDTO> tables = entry.getValue();

            if (tables != null && !tables.isEmpty()) {
                any = true;
                sb.append("\n• ").append(slot).append(" (còn ").append(tables.size()).append(" bàn)\n");

                // gợi ý tối đa 5 bàn
                sb.append(
                        tables.stream().limit(5)
                                .map(t -> "  - Bàn " + t.getTableNumber()
                                        + " (" + t.getCapacity() + " khách)"
                                        + (t.getLocation() != null && !t.getLocation().isBlank() ? " - " + t.getLocation() : "")
                                        + " [" + t.getTableType() + "]"
                                )
                                .collect(java.util.stream.Collectors.joining("\n"))
                ).append("\n");
            } else {
                sb.append("\n• ").append(slot).append(": hết bàn phù hợp\n");
            }
        }

        if (!any) {
            sb.append("\nHiện chưa thấy khung giờ nào còn bàn phù hợp trong ngày này.");
        }

        sb.append("\n\nMình đã ghi nhận nhu cầu của bạn. Để đặt chính thức, bạn vào mục Reservations/Đặt bàn nhé.")
                .append("\nBạn cần hỗ trợ gì nữa không?");

        return sb.toString();
    }

    private boolean isBookingIntent(String msg) {
        String m = msg.toLowerCase();
        return m.contains("bàn trống") || m.contains("ban trong")
                || m.contains("còn bàn") || m.contains("con ban")
                || m.contains("đặt bàn") || m.contains("dat ban")
                || m.contains("reservation") || m.contains("book");
    }

    private boolean isCancel(String msg) {
        String m = msg.toLowerCase().trim();
        return m.equals("hủy") || m.equals("huy") || m.equals("cancel") || m.equals("stop");
    }
    private Integer parseIntSafe(String s) {
        try {
            String digits = s.replaceAll("[^0-9]", "");
            if (digits.isBlank()) return null;
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return null;
        }
    }
}