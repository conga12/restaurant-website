package com.tt.Restaurant.controller;

import com.tt.Restaurant.chat.BookingState;
import com.tt.Restaurant.chat.ChatSessionStore;
import com.tt.Restaurant.chat.NaturalLanguageSlot;
import com.tt.Restaurant.chat.VietnameseDateTimeParser;
import com.tt.Restaurant.dto.AIChatRequest;
import com.tt.Restaurant.dto.AIChatResponse;
import com.tt.Restaurant.dto.AvailableTableDTO;
import com.tt.Restaurant.service.GeminiService;
import com.tt.Restaurant.service.TableAvailabilityService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

        if (isCancel(msg)) {
            chatSessionStore.clear(sessionId);
            return new AIChatResponse("Đã hủy. Bạn cần mình hỗ trợ gì nữa không?", true, "", "");
        }

        BookingState state = chatSessionStore.getOrCreate(sessionId);

        // Parse tự nhiên mỗi lần user nhắn
        NaturalLanguageSlot parsed = VietnameseDateTimeParser.parse(msg);
        applyParsedToState(state, parsed);

        // Nếu user đang trong flow => xử lý theo step nhưng vẫn nhận input tự nhiên
        if (state.getStep() != BookingState.Step.NONE) {
            return handleBookingFlowC2(state, sessionId, msg);
        }

        // Nếu message có intent check bàn trống / đặt bàn
        if (isAvailabilityIntent(msg)) {
            // nếu đã đủ date + guests => trả ngay
            if (hasDate(state) && state.getGuests() != null) {
                return replyAvailabilityAndClear(state, sessionId);
            }

            // thiếu gì hỏi đó
            if (!hasDate(state)) {
                state.setStep(BookingState.Step.ASK_DATE);
                return new AIChatResponse("Bạn muốn kiểm tra bàn trống ngày nào? (hôm nay/ngày mai/thứ 6/10-05-2026...)", false, "date", "");
            }

            state.setStep(BookingState.Step.ASK_GUESTS);
            return new AIChatResponse("Bạn đi mấy người? (VD: 2 người)", false, "guests", "");
        }

        // Fallback chat thường
        String reply = geminiService.chatSupport(msg);
        return new AIChatResponse(reply, true, "", "");
    }

    private AIChatResponse handleBookingFlowC2(BookingState state, String sessionId, String msg) {
        // Luồng hỏi thiếu gì -> nhận tự nhiên -> nếu đủ trả kết quả
        switch (state.getStep()) {
            case ASK_DATE -> {
                if (!hasDate(state)) {
                    return new AIChatResponse("Bạn cho mình ngày cụ thể nhé (hôm nay/ngày mai/thứ 6/2026-05-10).", false, "date", "");
                }
                state.setStep(BookingState.Step.ASK_GUESTS);

                if (state.getGuests() != null) {
                    return replyAvailabilityAndClear(state, sessionId);
                }
                return new AIChatResponse("Bạn đi mấy người? (VD: 2 người)", false, "guests", "");
            }
            case ASK_GUESTS -> {
                if (state.getGuests() == null) {
                    return new AIChatResponse("Bạn cho mình số người nhé (VD: 2 người / 4 khách).", false, "guests", "");
                }
                if (!hasDate(state)) {
                    state.setStep(BookingState.Step.ASK_DATE);
                    return new AIChatResponse("Bạn muốn kiểm tra ngày nào? (hôm nay/ngày mai/thứ 6...)", false, "date", "");
                }
                return replyAvailabilityAndClear(state, sessionId);
            }
            default -> {
                chatSessionStore.clear(sessionId);
                return new AIChatResponse("Mình bị lỗi luồng. Bạn gõ 'bàn trống' để bắt đầu lại nhé.", true, "", "");
            }
        }
    }

    private AIChatResponse replyAvailabilityAndClear(BookingState state, String sessionId) {
        LocalDate date = LocalDate.parse(state.getDate());
        int guests = state.getGuests();

        // Nếu có time (tối/19:30/7 giờ tối) -> check đúng giờ đó
        if (state.getTime() != null && !state.getTime().isBlank()) {
            LocalTime time = LocalTime.parse(state.getTime());
            List<AvailableTableDTO> tables = tableAvailabilityService.findAvailableTables(date, time, guests);
            String reply = buildAvailabilityReply(state.getDate(), state.getTime(), guests, tables);
            chatSessionStore.clear(sessionId);
            return new AIChatResponse(reply, true, "", "");
        }

        // Không có time -> check theo slots trong ngày
        List<LocalTime> slots = List.of(
                LocalTime.of(17, 0),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                LocalTime.of(21, 0)
        );

        Map<LocalTime, List<AvailableTableDTO>> byDay =
                tableAvailabilityService.findAvailableTablesByDay(date, guests, slots);

        String reply = buildAvailabilityByDayReply(state.getDate(), guests, byDay);

        chatSessionStore.clear(sessionId);
        return new AIChatResponse(reply, true, "", "");
    }

    private void applyParsedToState(BookingState state, NaturalLanguageSlot parsed) {
        if (parsed == null) return;

        if (parsed.getDate() != null) {
            state.setDate(parsed.getDate().toString()); // YYYY-MM-DD
        }
        if (parsed.getTime() != null) {
            state.setTime(parsed.getTime().toString()); // HH:mm[:ss]
        }
        if (parsed.getGuests() != null) {
            state.setGuests(parsed.getGuests());
        }
    }

    private boolean hasDate(BookingState state) {
        return state.getDate() != null && !state.getDate().isBlank();
    }

    private boolean isAvailabilityIntent(String msg) {
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

    private String buildAvailabilityReply(String date, String time, int guests, List<AvailableTableDTO> tables) {
        if (tables == null || tables.isEmpty()) {
            return "Kết quả kiểm tra bàn trống (" + guests + " người, " + date + " " + time + "):\n"
                    + "Hiện không thấy bàn trống phù hợp ở khung giờ này.\n"
                    + "Bạn có muốn đổi giờ hoặc đổi số người không?\n"
                    + "Bạn cần hỗ trợ gì nữa không?";
        }

        String list = tables.stream()
                .limit(10)
                .map(t -> "- Bàn " + t.getTableNumber()
                        + " (" + t.getCapacity() + " khách)"
                        + (t.getLocation() != null && !t.getLocation().isBlank() ? " - " + t.getLocation() : "")
                        + " [" + t.getTableType() + "]"
                )
                .collect(Collectors.joining("\n"));

        return "Kết quả kiểm tra bàn trống (" + guests + " người, " + date + " " + time + "):\n"
                + list
                + "\n\nBạn cần hỗ trợ gì nữa không?";
    }

    private String buildAvailabilityByDayReply(String date, int guests, Map<LocalTime, List<AvailableTableDTO>> availability) {
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

                sb.append(
                        tables.stream().limit(5)
                                .map(t -> "  - Bàn " + t.getTableNumber()
                                        + " (" + t.getCapacity() + " khách)"
                                        + (t.getLocation() != null && !t.getLocation().isBlank() ? " - " + t.getLocation() : "")
                                        + " [" + t.getTableType() + "]")
                                .collect(Collectors.joining("\n"))
                ).append("\n");
            } else {
                sb.append("\n• ").append(slot).append(": hết bàn phù hợp\n");
            }
        }

        if (!any) {
            sb.append("\nHiện chưa thấy khung giờ nào còn bàn phù hợp trong ngày này.");
        }

        sb.append("\nBạn cần hỗ trợ gì nữa không?");
        return sb.toString();
    }
}