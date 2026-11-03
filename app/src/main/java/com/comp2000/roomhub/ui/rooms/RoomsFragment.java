package com.comp2000.roomhub.ui.rooms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.Room;
import com.comp2000.roomhub.data.model.RoomType;
import com.comp2000.roomhub.databinding.FragmentRoomsBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.Nav;
import com.comp2000.roomhub.ui.common.Pickers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** All rooms overview: booked/vacant status relative to a visible, editable date and time slot. */
public class RoomsFragment extends Fragment {
    private FragmentRoomsBinding b;
    private RoomAdapter adapter;
    private LocalDate date;
    private LocalTime from;
    private final Runnable refresher = this::render;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentRoomsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        RoomHubRepository repo = RoomHubRepository.get();
        if (date == null) {
            date = repo.today();
            from = repo.now().toLocalTime().withMinute(0);
        }
        adapter = new RoomAdapter(room -> Nav.to(this, R.id.nav_room_detail, Nav.args("roomId", room.id)));
        b.list.setLayoutManager(new GridLayoutManager(requireContext(), getResources().getInteger(R.integer.room_grid_span)));
        b.list.setAdapter(adapter);

        b.filterAll.setText(getString(R.string.rooms_all) + " " + repo.getRooms(null).size());
        b.filterMeeting.setText(getString(R.string.rooms_meeting) + " " + repo.getRooms(RoomType.MEETING).size());
        b.filterSeminar.setText(getString(R.string.rooms_seminar) + " " + repo.getRooms(RoomType.SEMINAR).size());
        b.filters.setOnCheckedStateChangeListener((g, ids) -> render());

        b.dateButton.setOnClickListener(v -> Pickers.date(getChildFragmentManager(), getString(R.string.booking_date), date, false, d -> {
            date = d;
            render();
        }));
        b.timeButton.setOnClickListener(v -> Pickers.time(getChildFragmentManager(), getString(R.string.booking_start), from, t -> {
            from = t;
            render();
        }));
        repo.addListener(refresher);
        render();
    }

    private void render() {
        if (b == null) return;
        RoomType filter = b.filterMeeting.isChecked() ? RoomType.MEETING : b.filterSeminar.isChecked() ? RoomType.SEMINAR : null;
        LocalTime to = from.plusHours(1);
        List<Room> rooms = RoomHubRepository.get().getRooms(filter);
        int vacant = 0;
        for (Room r : rooms) if (RoomHubRepository.get().occupyingBooking(r, date, from, to) == null) vacant++;
        b.dateButton.setText(Formats.day(date));
        b.timeButton.setText(Formats.range(from, to));
        b.summary.setText(getString(R.string.rooms_summary, vacant, rooms.size()));
        adapter.submit(rooms, date, from, to);
    }

    @Override
    public void onDestroyView() {
        RoomHubRepository.get().removeListener(refresher);
        b = null;
        super.onDestroyView();
    }
}
