package com.comp2000.roomhub.ui.requests;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.BookingRequest;
import com.comp2000.roomhub.databinding.ItemRequestBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.StatusStyle;

import java.util.ArrayList;
import java.util.List;

/** Request cards with the clash check and inline decision buttons (CR4). */
public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.VH> {
    public interface Listener {
        void onOpen(BookingRequest r);

        void onApprove(BookingRequest r);

        void onDecline(BookingRequest r);
    }

    private final List<BookingRequest> items = new ArrayList<>();
    private final Listener listener;

    public RequestAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<BookingRequest> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    /** Existing booking that would clash if this booking request were approved, or null. */
    static Booking clashFor(BookingRequest r) {
        if (r.type != BookingRequest.Type.BOOKING) return null;
        Booking bk = r.booking;
        return RoomHubRepository.get().findConflict(bk.room, bk.date, bk.start, bk.end, bk.id);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BookingRequest r = items.get(position);
        ItemRequestBinding b = holder.b;
        Context c = b.getRoot().getContext();
        Booking bk = r.booking;
        b.avatar.setText(r.requester.initials());
        b.name.setText(r.requester.name);
        b.submitted.setText(r.requester.department + " · " + c.getString(R.string.req_requested, Formats.ago(r.submitted)));
        b.what.setText(bk.room.displayName());
        b.when.setText(Formats.when(bk.date, bk.start, bk.end) + " · " + bk.attendees + " people");
        b.note.setText("“" + r.note + "”");

        boolean booking = r.type == BookingRequest.Type.BOOKING;
        Booking clash = clashFor(r);
        b.clash.getRoot().setVisibility(booking ? View.VISIBLE : View.GONE);
        if (clash != null) {
            StatusStyle.apply(b.clash.getRoot(), StatusStyle.Kind.BOOKED, c.getString(R.string.req_clash, clash.title));
        } else {
            StatusStyle.apply(b.clash.getRoot(), StatusStyle.Kind.VACANT, c.getString(R.string.req_no_clash));
        }
        b.secondary.setText(booking ? R.string.req_decline : R.string.req_keep);
        b.primary.setText(booking ? R.string.req_approve : R.string.req_approve_cancel);
        // Error prevention: a clashing request cannot be approved from the list.
        b.primary.setEnabled(clash == null);
        b.primary.setOnClickListener(v -> listener.onApprove(r));
        b.secondary.setOnClickListener(v -> listener.onDecline(r));
        b.getRoot().setOnClickListener(v -> listener.onOpen(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRequestBinding b;

        VH(ItemRequestBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
