package com.comp2000.roomhub.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.Session;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.databinding.FragmentBookingConfirmedBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.UiKit;

/** Explicit closure for the booking task (Shneiderman: design dialogs to yield closure). */
public class BookingConfirmedFragment extends Fragment {
    private FragmentBookingConfirmedBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentBookingConfirmedBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Booking bk = RoomHubRepository.get().getBooking(requireArguments().getString("bookingId"));
        String forName = bk.bookedFor.id.equals(Session.user().id) ? getString(R.string.you_suffix, bk.bookedFor.name) : bk.bookedFor.name;
        UiKit.addDetailRow(b.summary, R.drawable.ic_meeting_room, getString(R.string.booking_room), bk.room.displayName());
        UiKit.addDetailRow(b.summary, R.drawable.ic_event, getString(R.string.detail_when), Formats.longDay(bk.date) + "\n" + Formats.range(bk.start, bk.end));
        UiKit.addDetailRow(b.summary, R.drawable.ic_person, getString(R.string.confirmed_for), forName);
        UiKit.addDetailRow(b.summary, R.drawable.ic_tag, getString(R.string.confirmed_reference), bk.reference);

        b.check.setScaleX(0.4f);
        b.check.setScaleY(0.4f);
        b.check.animate().scaleX(1f).scaleY(1f).setDuration(450).setInterpolator(new OvershootInterpolator()).start();

        b.actions.secondaryAction.setVisibility(View.VISIBLE);
        b.actions.secondaryAction.setText(R.string.confirmed_view);
        b.actions.secondaryAction.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("bookingId", bk.id);
            NavHostFragment.findNavController(this).navigate(R.id.nav_booking_detail, args,
                    new NavOptions.Builder().setPopUpTo(R.id.nav_booking_confirmed, true).build());
        });
        b.actions.primaryAction.setText(R.string.confirmed_done);
        b.actions.primaryAction.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack(R.id.nav_home, false));
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
