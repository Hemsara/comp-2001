package com.comp2000.roomhub.ui.booking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.BookingRequest;
import com.comp2000.roomhub.data.model.BookingStatus;
import com.comp2000.roomhub.databinding.FragmentBookingDetailBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.Nav;
import com.comp2000.roomhub.ui.common.StatusStyle;
import com.comp2000.roomhub.ui.common.UiKit;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDateTime;

/** Booking details with a guarded, undoable cancel action. */
public class BookingDetailFragment extends Fragment {
    private FragmentBookingDetailBinding b;
    private Booking booking;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentBookingDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        RoomHubRepository repo = RoomHubRepository.get();
        booking = repo.getBooking(requireArguments().getString("bookingId"));
        StatusStyle.apply(b.status, booking.status);
        b.title.setText(booking.title);

        UiKit.roomBadge(b.roomCard.badge, booking.room);
        b.roomCard.name.setText(booking.room.displayName());
        b.roomCard.meta.setText(UiKit.roomMeta(booking.room));
        b.roomCard.status.getRoot().setVisibility(View.GONE);
        b.roomCard.statusDetail.setText(booking.room.type.label);
        b.roomCard.getRoot().setOnClickListener(v -> Nav.to(this, R.id.nav_room_detail, Nav.args("roomId", booking.room.id)));

        UiKit.addDetailRow(b.rows, R.drawable.ic_event, getString(R.string.detail_when),
                Formats.longDay(booking.date) + " · " + Formats.range(booking.start, booking.end));
        UiKit.addDetailRow(b.rows, R.drawable.ic_person, getString(R.string.detail_booked_for), booking.bookedFor.name + " · " + booking.bookedFor.department);
        UiKit.addDetailRow(b.rows, R.drawable.ic_edit, getString(R.string.detail_booked_by), booking.bookedBy.name);
        UiKit.addDetailRow(b.rows, R.drawable.ic_group, getString(R.string.detail_attendees), booking.attendees + " of " + booking.room.capacity + " seats");
        UiKit.addDetailRow(b.rows, R.drawable.ic_tag, getString(R.string.detail_reference), booking.reference);

        LocalDateTime now = repo.now();
        boolean upcoming = !booking.date.atTime(booking.end).isBefore(now);
        if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CANCEL_REQUESTED) {
            BookingRequest req = findRequest();
            b.note.setVisibility(View.VISIBLE);
            b.note.setText(booking.status == BookingStatus.PENDING
                    ? booking.bookedFor.firstName() + " has requested this booking. It does not hold the room until approved."
                    : booking.bookedFor.firstName() + " has asked to cancel this booking.");
            b.actions.primaryAction.setText("Review request");
            b.actions.primaryAction.setOnClickListener(v -> {
                if (req != null) Nav.to(this, R.id.nav_request_detail, Nav.args("requestId", req.id));
            });
        } else if (booking.status == BookingStatus.CONFIRMED && upcoming) {
            if (booking.isOnBehalf()) {
                b.note.setVisibility(View.VISIBLE);
                b.note.setText("Booked on behalf of " + booking.bookedFor.name + ". They will be notified of any change.");
            }
            b.actions.primaryAction.setText(R.string.detail_cancel);
            b.actions.primaryAction.setIconResource(R.drawable.ic_cancel);
            b.actions.primaryAction.setOnClickListener(v -> confirmCancel());
        } else {
            b.actions.getRoot().setVisibility(View.GONE);
        }
    }

    private BookingRequest findRequest() {
        RoomHubRepository repo = RoomHubRepository.get();
        for (BookingRequest.Type t : BookingRequest.Type.values()) {
            for (BookingRequest r : repo.pendingRequests(t)) if (r.booking == booking) return r;
        }
        return null;
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.detail_cancel_title)
                .setMessage(getString(R.string.detail_cancel_body, booking.room.name,
                        Formats.when(booking.date, booking.start, booking.end), booking.bookedFor.name))
                .setNegativeButton(R.string.detail_keep, null)
                .setPositiveButton(R.string.detail_cancel, (d, w) -> {
                    RoomHubRepository.get().setStatus(booking, BookingStatus.CANCELLED);
                    View anchor = requireActivity().findViewById(R.id.nav_host);
                    Booking cancelled = booking;
                    Snackbar.make(anchor, R.string.detail_cancelled, Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, v -> RoomHubRepository.get().setStatus(cancelled, BookingStatus.CONFIRMED))
                            .show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
