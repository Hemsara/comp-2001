package com.comp2000.roomhub.ui.requests;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.fragment.app.Fragment;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.BookingRequest;
import com.comp2000.roomhub.databinding.DialogDeclineBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/** Shared approve/decline behaviour for the request list and request details (consistency). */
final class RequestActions {
    private RequestActions() {
    }

    static void approve(Fragment f, BookingRequest r, Runnable after) {
        RoomHubRepository.get().approve(r);
        String msg = r.type == BookingRequest.Type.BOOKING
                ? f.getString(R.string.req_approved, r.requester.firstName())
                : f.getString(R.string.req_cancel_approved, r.booking.room.name);
        undoable(f, msg, r);
        after.run();
    }

    static void decline(Fragment f, BookingRequest r, Runnable after) {
        if (r.type == BookingRequest.Type.CANCELLATION) {
            RoomHubRepository.get().decline(r, null);
            undoable(f, f.getString(R.string.req_cancel_kept, r.requester.firstName()), r);
            after.run();
            return;
        }
        DialogDeclineBinding d = DialogDeclineBinding.inflate(LayoutInflater.from(f.requireContext()));
        String[] reasons = f.getResources().getStringArray(R.array.decline_reasons);
        d.reason.setAdapter(new ArrayAdapter<>(f.requireContext(), android.R.layout.simple_list_item_1, reasons));
        d.reason.setText(reasons[0], false);
        d.messageLayout.setHint(f.getString(R.string.req_decline_message, r.requester.firstName()));
        new MaterialAlertDialogBuilder(f.requireContext())
                .setTitle(R.string.req_decline_title)
                .setView(d.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.req_decline, (dialog, w) -> {
                    RoomHubRepository.get().decline(r, String.valueOf(d.reason.getText()));
                    undoable(f, f.getString(R.string.req_declined, r.requester.firstName()), r);
                    after.run();
                })
                .show();
    }

    private static void undoable(Fragment f, String message, BookingRequest r) {
        View anchor = f.requireActivity().findViewById(R.id.nav_host);
        Snackbar.make(anchor, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> RoomHubRepository.get().undo(r))
                .show();
    }
}
