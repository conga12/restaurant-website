package com.tt.Restaurant.chat;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VietnameseDateTimeParser {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(?<h>\\d{1,2})\\s*(?:h|giờ|gio)?\\s*(?::(?<m>\\d{2}))?\\s*(?<ampm>sa|sáng|sang|chieu|chiều|toi|tối|dem|đêm|am|pm)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern DMY_PATTERN = Pattern.compile("(?<d>\\d{1,2})[\\/\\-.](?<mo>\\d{1,2})[\\/\\-.](?<y>\\d{4})");
    private static final Pattern YMD_PATTERN = Pattern.compile("(?<y>\\d{4})-(?<mo>\\d{2})-(?<d>\\d{2})");

    private static final Pattern GUESTS_PATTERN = Pattern.compile("(?<n>\\d{1,2})\\s*(?:người|nguoi|khách|khach)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static NaturalLanguageSlot parse(String message) {
        String raw = message == null ? "" : message.trim();
        String m = normalize(raw);

        NaturalLanguageSlot slot = new NaturalLanguageSlot();
        slot.setOriginal(raw);

        LocalDate today = LocalDate.now(ZONE);
        LocalTime now = LocalTime.now(ZONE);

        // guests
        Matcher gm = GUESTS_PATTERN.matcher(m);
        if (gm.find()) {
            slot.setGuests(Integer.parseInt(gm.group("n")));
        }

        // date
        LocalDate date = null;
        if (m.contains("hom nay") || m.equals("hn")) {
            date = today;
        } else if (m.contains("ngay mai") || m.equals("mai") || m.contains(" mai")) {
            date = today.plusDays(1);
        } else if (m.contains("ngay kia") || m.contains("mot")) {
            date = today.plusDays(2);
        } else {
            try {
                Matcher ymd = YMD_PATTERN.matcher(m);
                Matcher dmy = DMY_PATTERN.matcher(m);
                if (ymd.find()) {
                    date = LocalDate.of(
                            Integer.parseInt(ymd.group("y")),
                            Integer.parseInt(ymd.group("mo")),
                            Integer.parseInt(ymd.group("d"))
                    );
                } else if (dmy.find()) {
                    date = LocalDate.of(
                            Integer.parseInt(dmy.group("y")),
                            Integer.parseInt(dmy.group("mo")),
                            Integer.parseInt(dmy.group("d"))
                    );
                } else {
                    Integer dow = parseDayOfWeek(m); // 1=Mon..7=Sun
                    if (dow != null) {
                        DayOfWeek target = DayOfWeek.of(dow);
                        LocalDate candidate = today.with(TemporalAdjusters.nextOrSame(target));
                        if (m.contains("tuan sau")) {
                            candidate = today.with(TemporalAdjusters.next(target));
                        }
                        date = candidate;
                    }
                }
            } catch (Exception ignored) {}
        }
        slot.setDate(date);

        // time
        LocalTime time = null;

        // part-of-day defaults
        if (containsAny(m, "sang")) time = LocalTime.of(9, 0);
        if (containsAny(m, "trua")) time = LocalTime.of(12, 0);
        if (containsAny(m, "chieu")) time = LocalTime.of(15, 0);
        if (containsAny(m, "toi")) time = LocalTime.of(19, 0);

        Matcher tm = TIME_PATTERN.matcher(m);
        if (tm.find()) {
            int h = Integer.parseInt(tm.group("h"));
            int min = tm.group("m") == null ? 0 : Integer.parseInt(tm.group("m"));
            String ampm = tm.group("ampm") == null ? "" : normalize(tm.group("ampm"));

            if (ampm.contains("pm") || ampm.contains("toi") || ampm.contains("chieu")) {
                if (h >= 1 && h <= 11) h += 12;
            } else if (ampm.contains("am") || ampm.contains("sang")) {
                // keep
            } else {
                // heuristic: "7 giờ tối"/"tối" already handled; if "7 giờ" and user says "nay/mai" in afternoon -> +12
                if (h >= 1 && h <= 11 && now.isAfter(LocalTime.of(12, 0)) && containsAny(m, "nay", "toi", "chieu")) {
                    h += 12;
                }
            }

            try { time = LocalTime.of(h % 24, min); } catch (Exception ignored) {}
        }

        slot.setTime(time);
        return slot;
    }

    private static String normalize(String s) {
        return s.toLowerCase()
                .replace("đ", "d")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsAny(String s, String... keys) {
        for (String k : keys) if (s.contains(normalize(k))) return true;
        return false;
    }

    // 1..7 = Mon..Sun
    private static Integer parseDayOfWeek(String m) {
        if (m.contains("chu nhat") || m.contains(" cn")) return 7;
        if (m.contains("thu 2") || m.contains("thu hai")) return 1;
        if (m.contains("thu 3") || m.contains("thu ba")) return 2;
        if (m.contains("thu 4") || m.contains("thu tu")) return 3;
        if (m.contains("thu 5") || m.contains("thu nam")) return 4;
        if (m.contains("thu 6") || m.contains("thu sau")) return 5;
        if (m.contains("thu 7") || m.contains("thu bay")) return 6;
        return null;
    }
}