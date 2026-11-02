package com.comp2000.roomhub.ui.booking;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.BookingStatus;
import com.comp2000.roomhub.databinding.ItemBookingBinding;
import com.comp2000.roomhub.databinding.ItemSectionHeaderBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.StatusStyle;
import com.google.android.material.color.MaterialColors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Booking list with optional date section headers (items are either String headers or Bookings). */
public class BookingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnBookingClick {
        void onClick(Booking booking);
    }

    private static final int TYPE_HEADER = 0, TYPE_BOOKING = 1;
    private final List<Object> items = new ArrayList<>();
    private final OnBookingClick listener;

    public BookingAdapter(OnBookingClick listener) {
        this.listener = listener;
    }

    public void submit(List<Booking> bookings, boolean withHeaders) {
        items.clear();
        LocalDate last = null;
        for (Booking b : bookings) {
            if (withHeaders && !b.date.equals(last)) {
                String rel = Formats.day(b.date);
                items.add(rel.matches("Today|Tomorrow|Yesterday") ? rel + " · " + Formats.longDay(b.date) : Formats.longDay(b.date));
                last = b.date;
            }
            items.add(b);
        }
        notifyDataSetChanged();
    }

    public boolean isHeader(int position) {
        return items.get(position) instanceof String;
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ? TYPE_HEADER : TYPE_BOOKING;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) return new HeaderVH(ItemSectionHeaderBinding.inflate(inf, parent, false));
        return new BookingVH(ItemBookingBinding.inflate(inf, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).b.header.setText((String) item);
            return;
        }
        Booking bk = (Booking) item;
        ItemBookingBinding b = ((BookingVH) holder).b;
        b.start.setText(Formats.time(bk.start));
        b.end.setText(Formats.time(bk.end));
        b.title.setText(bk.title);
        b.room.setText(bk.room.displayName());
        b.person.setText(bk.isOnBehalf()
                ? b.getRoot().getContext().getString(R.string.bookings_for, bk.bookedFor.name) + " · by " + bk.bookedBy.firstName()
                : bk.bookedFor.name);
        StatusStyle.apply(b.status.getRoot(), bk.status);
        int accent = bk.status == BookingStatus.CONFIRMED
                ? MaterialColors.getColor(b.accent, androidx.appcompat.R.attr.colorPrimary)
                : MaterialColors.getColor(b.accent, com.google.android.material.R.attr.colorOutline);
        b.accent.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        b.getRoot().setContentDescription(bk.title + ", " + bk.room.displayName() + ", "
                + Formats.when(bk.date, bk.start, bk.end) + ", " + bk.status.label);
        b.getRoot().setOnClickListener(v -> listener.onClick(bk));
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final ItemSectionHeaderBinding b;

        HeaderVH(ItemSectionHeaderBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    static class BookingVH extends RecyclerView.ViewHolder {
        final ItemBookingBinding b;

        BookingVH(ItemBookingBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
