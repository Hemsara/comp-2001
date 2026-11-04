package com.comp2000.roomhub.ui.requests;

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
import com.comp2000.roomhub.databinding.FragmentRequestDetailBinding;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.StatusStyle;
import com.comp2000.roomhub.ui.common.UiKit;

/** CR4: full context for a decision – requester, purpose and the room's schedule that day. */
public class RequestDetailFragment extends Fragment {
    private FragmentRequestDetailBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentRequestDetailBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        BookingRequest r = RoomHubRepository.get().getRequest(requireArguments().getString("requestId"));
        Booking bk = r.booking;
        boolean booking = r.type == BookingRequest.Type.BOOKING;

        b.avatar.setText(r.requester.initials());
        b.name.setText(r.requester.name);
        b.sub.setText(r.requester.department + " · " + getString(R.string.req_requested, Formats.ago(r.submitted)));
        StatusStyle.apply(b.type, StatusStyle.Kind.PENDING, booking ? "Booking request" : "Cancellation request");

        UiKit.addDetailRow(b.rows, R.drawable.ic_meeting_room, getString(R.string.booking_room), bk.room.displayName() + " · " + bk.room.capacity + " seats");
        UiKit.addDetailRow(b.rows, R.drawable.ic_event, getString(R.string.detail_when), Formats.longDay(bk.date) + " · " + Formats.range(bk.start, bk.end));
        UiKit.addDetailRow(b.rows, R.drawable.ic_edit, getString(R.string.booking_title), bk.title);
        UiKit.addDetailRow(b.rows, R.drawable.ic_chat, booking ? getString(R.string.req_purpose) : "Reason", r.note);
        UiKit.addDetailRow(b.rows, R.drawable.ic_group, getString(R.string.detail_attendees), String.valueOf(bk.attendees));

        Booking clash = RequestAdapter.clashFor(r);
        b.scheduleTitle.setText(getString(R.string.req_day_schedule) + " · " + Formats.day(bk.date));
        b.strip.setBookings(RoomHubRepository.get().bookingsForRoom(bk.room, bk.date), bk.id);
        b.strip.setSelection(bk.start, bk.end, clash != null);
        if (!booking) {
            b.clash.setVisibility(View.GONE);
        } else if (clash != null) {
            StatusStyle.apply(b.clash, StatusStyle.Kind.BOOKED, getString(R.string.req_clash, clash.title));
            b.clashDetail.setVisibility(View.VISIBLE);
            b.clashDetail.setText(getString(R.string.req_clash_block, clash.bookedFor.name, Formats.range(clash.start, clash.end)));
        } else {
            StatusStyle.apply(b.clash, StatusStyle.Kind.VACANT, getString(R.string.req_no_clash));
        }

        Runnable back = () -> NavHostFragment.findNavController(this).popBackStack();
        boolean decided = r.state != BookingRequest.State.PENDING;
        b.actions.secondaryAction.setVisibility(View.VISIBLE);
        b.actions.secondaryAction.setText(booking ? getString(R.string.req_decline) + "…" : getString(R.string.req_keep));
        b.actions.secondaryAction.setEnabled(!decided);
        b.actions.secondaryAction.setOnClickListener(v -> RequestActions.decline(this, r, back));
        b.actions.primaryAction.setText(booking ? R.string.req_approve : R.string.req_approve_cancel);
        b.actions.primaryAction.setEnabled(!decided && clash == null);
        b.actions.primaryAction.setOnClickListener(v -> RequestActions.approve(this, r, back));
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
