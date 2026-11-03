package com.comp2000.roomhub.ui.rooms;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.Room;
import com.comp2000.roomhub.databinding.ItemRoomBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.StatusStyle;
import com.comp2000.roomhub.ui.common.UiKit;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Rooms with their booked/vacant status for the selected date and time slot. */
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.VH> {
    public interface OnRoomClick {
        void onClick(Room room);
    }

    private final List<Room> rooms = new ArrayList<>();
    private final OnRoomClick listener;
    private LocalDate date;
    private LocalTime from, to;

    public RoomAdapter(OnRoomClick listener) {
        this.listener = listener;
    }

    public void submit(List<Room> list, LocalDate date, LocalTime from, LocalTime to) {
        rooms.clear();
        rooms.addAll(list);
        this.date = date;
        this.from = from;
        this.to = to;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRoomBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Room room = rooms.get(position);
        ItemRoomBinding b = holder.b;
        UiKit.roomBadge(b.badge, room);
        b.name.setText(room.displayName());
        b.meta.setText(UiKit.roomMeta(room));
        Booking occupying = RoomHubRepository.get().occupyingBooking(room, date, from, to);
        String status;
        if (occupying == null) {
            status = b.getRoot().getContext().getString(R.string.rooms_vacant);
            StatusStyle.apply(b.status.getRoot(), StatusStyle.Kind.VACANT, status);
            b.statusDetail.setText(R.string.rooms_free_until);
        } else {
            status = b.getRoot().getContext().getString(R.string.rooms_booked);
            StatusStyle.apply(b.status.getRoot(), StatusStyle.Kind.BOOKED, status);
            b.statusDetail.setText(b.getRoot().getContext().getString(R.string.rooms_booked_until,
                    Formats.time(occupying.end), occupying.bookedFor.name));
        }
        b.getRoot().setContentDescription(room.displayName() + ", " + room.capacity + " seats, " + status
                + ". " + b.statusDetail.getText());
        b.getRoot().setOnClickListener(v -> listener.onClick(room));
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRoomBinding b;

        VH(ItemRoomBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
