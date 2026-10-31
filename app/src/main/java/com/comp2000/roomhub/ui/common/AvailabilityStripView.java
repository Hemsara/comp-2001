package com.comp2000.roomhub.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.BookingStatus;
import com.google.android.material.color.MaterialColors;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Day timeline (08:00–18:00) showing booked and pending blocks plus the user's selection.
 * Implements refinement CR1: clashes are visible while times are chosen, before submitting.
 */
public class AvailabilityStripView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint booked = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pending = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final List<Booking> blocks = new ArrayList<>();
    private LocalTime selStart, selEnd;
    private boolean selConflict;
    private String excludeId;
    private final float density;

    public AvailabilityStripView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        track.setColor(ContextCompat.getColor(context, R.color.timeline_track));
        booked.setColor(ContextCompat.getColor(context, R.color.timeline_booked));
        pending.setColor(ContextCompat.getColor(context, R.color.timeline_pending));
        pending.setStyle(Paint.Style.STROKE);
        pending.setStrokeWidth(2 * density);
        pending.setPathEffect(new DashPathEffect(new float[]{4 * density, 3 * density}, 0));
        selection.setStyle(Paint.Style.STROKE);
        selection.setStrokeWidth(3 * density);
        grid.setColor(ContextCompat.getColor(context, R.color.timeline_grid));
        grid.setStrokeWidth(density);
        label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, getResources().getDisplayMetrics()));
        label.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        label.setTextAlign(Paint.Align.CENTER);
        setContentDescription(null);
    }

    public void setBookings(List<Booking> list, @Nullable String excludeBookingId) {
        blocks.clear();
        blocks.addAll(list);
        excludeId = excludeBookingId;
        updateDescription();
        invalidate();
    }

    public void setSelection(@Nullable LocalTime start, @Nullable LocalTime end, boolean conflict) {
        selStart = start;
        selEnd = end;
        selConflict = conflict;
        updateDescription();
        invalidate();
    }

    private void updateDescription() {
        StringBuilder sb = new StringBuilder("Room schedule. ");
        for (Booking b : blocks) {
            if (b.id.equals(excludeId)) continue;
            sb.append(b.status == BookingStatus.PENDING ? "Requested " : "Booked ")
                    .append(Formats.range(b.start, b.end)).append(". ");
        }
        if (selStart != null) {
            sb.append("Your selection ").append(Formats.range(selStart, selEnd))
                    .append(selConflict ? ", clashes." : ", free.");
        }
        setContentDescription(sb.toString());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = (int) (52 * density);
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    private float x(LocalTime t) {
        float total = (RoomHubRepository.DAY_END.toSecondOfDay() - RoomHubRepository.DAY_START.toSecondOfDay());
        float pos = (t.toSecondOfDay() - RoomHubRepository.DAY_START.toSecondOfDay()) / total;
        float pad = 4 * density;
        return pad + Math.max(0f, Math.min(1f, pos)) * (getWidth() - 2 * pad);
    }

    @Override
    protected void onDraw(Canvas c) {
        float top = 6 * density, bottom = 30 * density, r = 6 * density;
        rect.set(x(RoomHubRepository.DAY_START), top, x(RoomHubRepository.DAY_END), bottom);
        c.drawRoundRect(rect, r, r, track);
        for (int h = 8; h <= 18; h++) {
            float gx = x(LocalTime.of(h, 0));
            c.drawLine(gx, bottom, gx, bottom + 4 * density, grid);
            if (h % 2 == 0) c.drawText(String.valueOf(h), gx, bottom + 16 * density, label);
        }
        for (Booking b : blocks) {
            if (b.id.equals(excludeId)) continue;
            rect.set(x(b.start), top, x(b.end), bottom);
            if (b.status == BookingStatus.PENDING) {
                rect.inset(density, density);
                c.drawRoundRect(rect, r, r, pending);
            } else {
                c.drawRoundRect(rect, r, r, booked);
            }
        }
        if (selStart != null && selEnd != null && selEnd.isAfter(selStart)) {
            selection.setColor(MaterialColors.getColor(this, selConflict
                    ? androidx.appcompat.R.attr.colorError : androidx.appcompat.R.attr.colorPrimary));
            rect.set(x(selStart), top - 3 * density, x(selEnd), bottom + 3 * density);
            c.drawRoundRect(rect, r, r, selection);
        }
    }
}
