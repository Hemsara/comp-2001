package com.comp2000.roomhub.ui.booking;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.Session;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.Room;
import com.comp2000.roomhub.data.model.User;
import com.comp2000.roomhub.databinding.FragmentCreateBookingBinding;
import com.comp2000.roomhub.databinding.ViewBookingFieldsBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.Pickers;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Create booking (for self or on behalf of another user). Validates as the user edits:
 * time order, office hours, past times and – most importantly – overlaps with existing bookings.
 */
public class CreateBookingFragment extends Fragment {
    private FragmentCreateBookingBinding b;
    private ViewBookingFieldsBinding f;
    private final RoomHubRepository repo = RoomHubRepository.get();
    private Room room;
    private User person;
    private LocalDate date;
    private LocalTime start, end;
    private boolean saving;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentCreateBookingBinding.inflate(inflater, container, false);
        f = b.fields;
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (room == null) initDefaults(args);

        List<Room> rooms = repo.getRooms(null);
        f.room.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, rooms));
        f.room.setText(room.toString(), false);
        f.room.setOnItemClickListener((p, v, pos, id) -> {
            room = rooms.get(pos);
            validate();
        });

        List<User> people = repo.getEmployees();
        f.person.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, people));
        f.person.setOnItemClickListener((p, v, pos, id) -> {
            person = (User) p.getItemAtPosition(pos);
            f.person.setText(person.toString(), false);
            f.personLayout.setError(null);
            validate();
        });
        f.person.addTextChangedListener(new SimpleWatcher(() -> {
            if (person != null && !person.toString().contentEquals(f.person.getText())) {
                person = null;
                validate();
            }
        }));

        f.forGroup.addOnButtonCheckedListener((g, id, checked) -> {
            if (checked) validate();
        });
        if (args.getBoolean("onBehalf")) f.forGroup.check(R.id.for_other);

        f.date.setOnClickListener(v -> Pickers.date(getChildFragmentManager(), getString(R.string.booking_date), date, true, d -> {
            date = d;
            validate();
        }));
        f.start.setOnClickListener(v -> Pickers.time(getChildFragmentManager(), getString(R.string.booking_start), start, t -> {
            long len = java.time.Duration.between(start, end).toMinutes();
            start = t;
            end = t.plusMinutes(len > 0 ? len : 60);
            validate();
        }));
        f.end.setOnClickListener(v -> Pickers.time(getChildFragmentManager(), getString(R.string.booking_end), end, t -> {
            end = t;
            validate();
        }));
        f.title.addTextChangedListener(new SimpleWatcher(() -> f.titleLayout.setError(null)));
        f.attendees.addTextChangedListener(new SimpleWatcher(this::validate));

        b.actions.primaryAction.setOnClickListener(v -> submit());
        validate();
    }

    private void initDefaults(Bundle args) {
        String roomId = args.getString("roomId");
        room = roomId != null ? repo.getRoom(roomId) : repo.getRooms(null).get(2);
        String d = args.getString("date");
        date = d != null ? LocalDate.parse(d) : repo.today();
        String s = args.getString("start");
        if (s != null) {
            start = LocalTime.parse(s);
        } else {
            // Next half hour after now.
            LocalTime now = repo.now().toLocalTime();
            start = now.withMinute(now.getMinute() < 30 ? 30 : 0).withSecond(0);
            if (now.getMinute() >= 30) start = start.plusHours(1);
            if (!date.equals(repo.today())) start = LocalTime.of(9, 0);
        }
        end = start.plusHours(1);
    }

    private boolean forOther() {
        return f.forGroup.getCheckedButtonId() == R.id.for_other;
    }

    /** Re-evaluates every rule and updates field errors, the timeline, the banner and the button. */
    private void validate() {
        if (b == null) return;
        f.date.setText(Formats.day(date) + " · " + Formats.longDay(date));
        f.start.setText(Formats.time(start));
        f.end.setText(Formats.time(end));

        boolean other = forOther();
        f.personLayout.setVisibility(other ? View.VISIBLE : View.GONE);
        f.personNotice.setVisibility(other && person != null ? View.VISIBLE : View.GONE);
        if (person != null) f.personNotice.setText(getString(R.string.booking_person_notice, person.firstName()));

        String timeError = null;
        if (!end.isAfter(start)) timeError = getString(R.string.booking_err_time);
        else if (start.isBefore(RoomHubRepository.DAY_START) || end.isAfter(RoomHubRepository.DAY_END))
            timeError = getString(R.string.booking_err_hours);
        else if (LocalDateTime.of(date, start).isBefore(repo.now().withMinute(0)))
            timeError = getString(R.string.booking_err_past);
        f.endLayout.setError(timeError);

        Booking conflict = timeError == null ? repo.findConflict(room, date, start, end, null) : null;
        b.availability.availTitle.setText(getString(R.string.booking_availability) + " · " + room.name + " · " + Formats.day(date));
        b.availability.strip.setBookings(repo.bookingsForRoom(room, date), null);
        b.availability.strip.setSelection(start, end, conflict != null || timeError != null);
        b.availability.suggestions.removeAllViews();

        if (timeError != null) {
            banner(false, timeError);
        } else if (conflict != null) {
            banner(false, getString(R.string.booking_conflict, room.name, Formats.range(conflict.start, conflict.end), conflict.title));
            LocalTime next = repo.nextFreeStart(room, date, start, end);
            if (next != null) {
                LocalTime nextEnd = next.plusMinutes(java.time.Duration.between(start, end).toMinutes());
                addSuggestion(getString(R.string.booking_suggest_time, Formats.range(next, nextEnd)), () -> {
                    start = next;
                    end = nextEnd;
                    validate();
                });
            }
            List<Room> free = repo.freeRooms(date, start, end, room);
            for (int i = 0; i < Math.min(2, free.size()); i++) {
                Room alt = free.get(i);
                addSuggestion(getString(R.string.booking_suggest_room, alt.name + " (" + alt.capacity + ")"), () -> {
                    room = alt;
                    f.room.setText(alt.toString(), false);
                    validate();
                });
            }
        } else {
            banner(true, getString(R.string.booking_free, room.name, Formats.range(start, end)));
        }

        int people = parseInt(String.valueOf(f.attendees.getText()));
        f.attendeesLayout.setHelperText(people > room.capacity
                ? getString(R.string.booking_warn_capacity, room.name, room.capacity) : null);

        boolean canSubmit = timeError == null && conflict == null && !saving;
        b.actions.primaryAction.setEnabled(canSubmit);
        if (!saving) {
            b.actions.primaryAction.setText(other && person != null
                    ? getString(R.string.booking_submit_for, room.name, person.name)
                    : canSubmit ? getString(R.string.booking_submit, room.name, Formats.range(start, end))
                    : getString(R.string.booking_submit_generic));
        }
    }

    private void banner(boolean ok, String text) {
        int fg = ContextCompat.getColor(requireContext(), ok ? R.color.status_vacant : R.color.status_booked);
        int bg = ContextCompat.getColor(requireContext(), ok ? R.color.status_vacant_container : R.color.status_booked_container);
        b.availability.statusCard.setCardBackgroundColor(bg);
        b.availability.statusText.setText(text);
        b.availability.statusText.setTextColor(fg);
        b.availability.statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(ok ? R.drawable.ic_check_circle : R.drawable.ic_warning, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(b.availability.statusText, ColorStateList.valueOf(fg));
    }

    private void addSuggestion(String text, Runnable action) {
        Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        chip.setText(text);
        chip.setCheckable(false);
        chip.setChipIconResource(R.drawable.ic_schedule);
        chip.setOnClickListener(v -> action.run());
        b.availability.suggestions.addView(chip);
    }

    private void submit() {
        boolean ok = true;
        if (TextUtils.isEmpty(f.title.getText())) {
            f.titleLayout.setError(getString(R.string.booking_err_title));
            ok = false;
        }
        if (forOther() && person == null) {
            f.personLayout.setError(getString(R.string.booking_err_person));
            ok = false;
        }
        if (!ok) return;

        saving = true;
        CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(requireContext(), null, 0,
                com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall);
        spec.indicatorColors = new int[]{MaterialColors.getColor(b.actions.primaryAction, com.google.android.material.R.attr.colorOnPrimary)};
        b.actions.primaryAction.setIcon(IndeterminateDrawable.createCircularDrawable(requireContext(), spec));
        b.actions.primaryAction.setText(R.string.booking_saving);
        b.actions.primaryAction.setEnabled(false);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (b == null) return;
            User me = Session.user();
            int people = Math.max(1, parseInt(String.valueOf(f.attendees.getText())));
            Booking created = repo.createBooking(room, String.valueOf(f.title.getText()).trim(), date, start, end,
                    me, forOther() ? person : me, people);
            Bundle args = new Bundle();
            args.putString("bookingId", created.id);
            NavHostFragment.findNavController(this).navigate(R.id.nav_booking_confirmed, args,
                    new NavOptions.Builder().setPopUpTo(R.id.nav_create_booking, true).build());
        }, 800);
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void onDestroyView() {
        b = null;
        f = null;
        super.onDestroyView();
    }

    /** TextWatcher that only cares about "text changed". */
    static class SimpleWatcher implements TextWatcher {
        private final Runnable r;

        SimpleWatcher(Runnable r) {
            this.r = r;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            r.run();
        }
    }
}
