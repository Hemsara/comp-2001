package com.comp2000.roomhub.ui.common;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.model.Room;
import com.comp2000.roomhub.data.model.RoomType;
import com.comp2000.roomhub.databinding.ViewDetailRowBinding;
import com.google.android.material.color.MaterialColors;

/** Small helpers that keep repeated components visually identical across screens. */
public final class UiKit {
    private UiKit() {
    }

    /** Room code badge: primary container for meeting rooms, secondary container for seminar rooms. */
    public static void roomBadge(TextView badge, Room room) {
        boolean meeting = room.type == RoomType.MEETING;
        badge.setText(room.code);
        badge.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(badge, meeting
                ? com.google.android.material.R.attr.colorPrimaryContainer
                : com.google.android.material.R.attr.colorSecondaryContainer)));
        badge.setTextColor(MaterialColors.getColor(badge, meeting
                ? com.google.android.material.R.attr.colorOnPrimaryContainer
                : com.google.android.material.R.attr.colorOnSecondaryContainer));
    }

    public static ViewDetailRowBinding addDetailRow(ViewGroup parent, @DrawableRes int icon, CharSequence label, CharSequence value) {
        ViewDetailRowBinding row = ViewDetailRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, true);
        row.icon.setImageDrawable(ContextCompat.getDrawable(parent.getContext(), icon));
        row.label.setText(label);
        row.value.setText(value);
        row.getRoot().setContentDescription(label + ": " + value);
        return row;
    }

    public static String roomMeta(Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append(room.capacity).append(" seats · ").append(room.floor);
        if (!room.amenities.isEmpty()) sb.append(" · ").append(room.amenities.get(0));
        return sb.toString();
    }

    public static int dp(android.content.Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    public static int statusColor(android.content.Context c, boolean ok) {
        return ContextCompat.getColor(c, ok ? R.color.status_vacant : R.color.status_booked);
    }
}
