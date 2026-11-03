package com.comp2000.roomhub.ui.rooms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.Room;
import com.comp2000.roomhub.data.model.RoomType;
import com.comp2000.roomhub.databinding.FragmentRoomDetailBinding;
import com.comp2000.roomhub.databinding.ItemBookingBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.Nav;
import com.comp2000.roomhub.ui.common.StatusStyle;
import com.comp2000.roomhub.ui.common.UiKit;
import com.google.android.material.chip.Chip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Room facts, facilities and day schedule (managers see who holds each booking). */
public class RoomDetailFragment extends Fragment {
    private FragmentRoomDetailBinding b;
    private Room room;
    private LocalDate day;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentRoomDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        RoomHubRepository repo = RoomHubRepository.get();
        room = repo.getRoom(requireArguments().getString("roomId"));
        if (day == null) day = repo.today();
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(room.displayName());

        b.info.heroCode.setText((room.type.label + " · " + room.code).toUpperCase());
        b.info.heroName.setText(room.name);
        LocalDateTime now = repo.now();
        Booking current = repo.occupyingBooking(room, now.toLocalDate(), now.toLocalTime(), now.toLocalTime().plusMinutes(1));
        if (current == null) StatusStyle.apply(b.info.heroStatus, StatusStyle.Kind.VACANT, "Vacant now");
        else StatusStyle.apply(b.info.heroStatus, StatusStyle.Kind.BOOKED, "Booked until " + Formats.time(current.end));
        if (room.type == RoomType.SEMINAR) b.info.hero.setBackgroundResource(R.drawable.bg_hero_seminar);

        UiKit.addDetailRow(b.info.facts, R.drawable.ic_group, "Capacity", room.capacity + " people");
        UiKit.addDetailRow(b.info.facts, R.drawable.ic_layers, "Location", room.floor);
        for (String a : room.amenities) {
            Chip chip = new Chip(requireContext());
            chip.setText(a);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setChipIconResource(a.contains("Video") ? R.drawable.ic_videocam
                    : a.contains("Whiteboard") ? R.drawable.ic_whiteboard : R.drawable.ic_tv);
            b.info.amenities.addView(chip);
        }

        b.schedule.prevDay.setOnClickListener(v -> shiftDay(-1));
        b.schedule.nextDay.setOnClickListener(v -> shiftDay(1));
        b.actions.primaryAction.setText(R.string.room_book);
        b.actions.primaryAction.setIconResource(R.drawable.ic_add);
        b.actions.primaryAction.setOnClickListener(v -> {
            Bundle args = Nav.args("roomId", room.id);
            args.putString("date", day.toString());
            Nav.to(this, R.id.nav_create_booking, args);
        });
        renderSchedule();
    }

    private void shiftDay(int delta) {
        day = day.plusDays(delta);
        renderSchedule();
    }

    private void renderSchedule() {
        b.schedule.day.setText(Formats.day(day));
        b.schedule.prevDay.setEnabled(day.isAfter(RoomHubRepository.get().today()));
        List<Booking> list = RoomHubRepository.get().bookingsForRoom(room, day);
        b.schedule.strip.setBookings(list, null);
        b.schedule.dayList.removeAllViews();
        for (Booking bk : list) {
            ItemBookingBinding row = ItemBookingBinding.inflate(getLayoutInflater(), b.schedule.dayList, true);
            row.start.setText(Formats.time(bk.start));
            row.end.setText(Formats.time(bk.end));
            row.title.setText(bk.title);
            row.room.setText(bk.bookedFor.name);
            row.person.setVisibility(View.GONE);
            StatusStyle.apply(row.status.getRoot(), bk.status);
            row.getRoot().setOnClickListener(v -> Nav.to(this, R.id.nav_booking_detail, Nav.args("bookingId", bk.id)));
        }
        b.schedule.dayEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
