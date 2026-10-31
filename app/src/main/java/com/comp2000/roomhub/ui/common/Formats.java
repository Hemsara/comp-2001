package com.comp2000.roomhub.ui.common;

import com.comp2000.roomhub.data.RoomHubRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Consistent date/time wording across every screen (UK format, 24-hour clock). */
public final class Formats {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK);
    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.UK);

    private Formats() {
    }

    public static String time(LocalTime t) {
        return TIME.format(t);
    }

    public static String range(LocalTime s, LocalTime e) {
        return time(s) + "–" + time(e);
    }

    /** "Today", "Tomorrow", "Yesterday" or e.g. "Fri 14 Nov". */
    public static String day(LocalDate d) {
        LocalDate today = RoomHubRepository.get().today();
        long diff = ChronoUnit.DAYS.between(today, d);
        if (diff == 0) return "Today";
        if (diff == 1) return "Tomorrow";
        if (diff == -1) return "Yesterday";
        return DAY.format(d);
    }

    public static String longDay(LocalDate d) {
        return LONG_DAY.format(d);
    }

    public static String when(LocalDate d, LocalTime s, LocalTime e) {
        return day(d) + " · " + range(s, e);
    }

    public static String ago(LocalDateTime t) {
        LocalDateTime now = RoomHubRepository.get().now();
        long min = ChronoUnit.MINUTES.between(t, now);
        if (min < 60) return Math.max(min, 1) + " min ago";
        long h = min / 60;
        if (h < 24) return h + " h ago";
        return day(t.toLocalDate());
    }

    public static String greeting(LocalTime t) {
        if (t.getHour() < 12) return "Good morning";
        if (t.getHour() < 18) return "Good afternoon";
        return "Good evening";
    }
}
