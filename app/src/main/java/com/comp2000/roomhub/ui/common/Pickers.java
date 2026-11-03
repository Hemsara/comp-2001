package com.comp2000.roomhub.ui.common;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.function.Consumer;

/** Native Material date and time pickers (recognition rather than typed dates). */
public final class Pickers {
    private Pickers() {
    }

    public static void date(FragmentManager fm, String title, LocalDate current, boolean futureOnly, Consumer<LocalDate> onPicked) {
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker()
                .setTitleText(title)
                .setSelection(current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
        if (futureOnly) {
            long today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            builder.setCalendarConstraints(new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(today)).build());
        }
        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(ms ->
                onPicked.accept(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()));
        picker.show(fm, "date");
    }

    public static void time(FragmentManager fm, String title, LocalTime current, Consumer<LocalTime> onPicked) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(current.getHour())
                .setMinute(current.getMinute())
                .setTitleText(title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            // Snap to 15-minute steps, which matches how rooms are normally booked.
            int minute = (picker.getMinute() / 15) * 15;
            onPicked.accept(LocalTime.of(picker.getHour(), minute));
        });
        picker.show(fm, "time");
    }
}
