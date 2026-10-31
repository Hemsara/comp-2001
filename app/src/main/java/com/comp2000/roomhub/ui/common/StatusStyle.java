package com.comp2000.roomhub.ui.common;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.model.BookingStatus;

/**
 * Applies the design-system status pill. Status is always communicated by colour, icon AND text
 * so it remains accessible to users with colour-vision deficiencies.
 */
public final class StatusStyle {
    public enum Kind { VACANT, BOOKED, PENDING, NEUTRAL }

    private StatusStyle() {
    }

    public static void apply(TextView pill, Kind kind, String text) {
        @ColorRes int fg, bg;
        @DrawableRes int icon;
        switch (kind) {
            case VACANT:
                fg = R.color.status_vacant; bg = R.color.status_vacant_container; icon = R.drawable.ic_check_circle; break;
            case BOOKED:
                fg = R.color.status_booked; bg = R.color.status_booked_container; icon = R.drawable.ic_event; break;
            case PENDING:
                fg = R.color.status_pending; bg = R.color.status_pending_container; icon = R.drawable.ic_hourglass; break;
            default:
                fg = R.color.status_neutral; bg = R.color.status_neutral_container; icon = R.drawable.ic_cancel; break;
        }
        int fgColor = ContextCompat.getColor(pill.getContext(), fg);
        pill.setText(text);
        pill.setTextColor(fgColor);
        pill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(pill.getContext(), bg)));
        Drawable d = ContextCompat.getDrawable(pill.getContext(), icon).mutate();
        int size = Math.round(14 * pill.getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, size, size);
        pill.setCompoundDrawablesRelative(d, null, null, null);
        TextViewCompat.setCompoundDrawableTintList(pill, ColorStateList.valueOf(fgColor));
    }

    public static void apply(TextView pill, BookingStatus status) {
        switch (status) {
            case CONFIRMED: apply(pill, Kind.VACANT, status.label); break;
            case PENDING:
            case CANCEL_REQUESTED: apply(pill, Kind.PENDING, status.label); break;
            default: apply(pill, Kind.NEUTRAL, status.label); break;
        }
    }
}
