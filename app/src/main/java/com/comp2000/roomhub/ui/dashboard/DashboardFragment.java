package com.comp2000.roomhub.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.Session;
import com.comp2000.roomhub.data.model.BookingRequest;
import com.comp2000.roomhub.databinding.FragmentDashboardBinding;
import com.comp2000.roomhub.databinding.ViewStatCardBinding;
import com.comp2000.roomhub.ui.booking.BookingAdapter;
import com.comp2000.roomhub.ui.common.Formats;
import com.comp2000.roomhub.ui.common.Nav;

import java.time.LocalDateTime;

/** Manager home: status overview, pending decisions and today's schedule. */
public class DashboardFragment extends Fragment {
    private FragmentDashboardBinding b;
    private BookingAdapter adapter;
    private final Runnable refresher = this::render;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentDashboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        adapter = new BookingAdapter(bk -> navigate(R.id.nav_booking_detail, bookingArgs(bk.id)));
        b.todayList.setLayoutManager(new GridLayoutManager(requireContext(), getResources().getInteger(R.integer.list_grid_span)));
        b.todayList.setAdapter(adapter);

        b.requestsBanner.setOnClickListener(v -> openRequests(0));
        b.bannerAction.setOnClickListener(v -> openRequests(0));
        b.newBooking.setOnClickListener(v -> navigate(R.id.nav_create_booking, null));
        b.seeAll.setOnClickListener(v -> Nav.toTopLevel(this, R.id.nav_bookings, null));
        b.stats.statVacant.getRoot().setOnClickListener(v -> Nav.toTopLevel(this, R.id.nav_rooms, null));
        b.stats.statToday.getRoot().setOnClickListener(v -> Nav.toTopLevel(this, R.id.nav_bookings, null));
        b.stats.statRequests.getRoot().setOnClickListener(v -> openRequests(0));
        b.stats.statCancellations.getRoot().setOnClickListener(v -> openRequests(1));

        RoomHubRepository.get().addListener(refresher);
        render();
    }

    private void render() {
        if (b == null) return;
        RoomHubRepository repo = RoomHubRepository.get();
        LocalDateTime now = repo.now();
        b.greeting.setText(Formats.greeting(now.toLocalTime()) + ", " + Session.user().firstName());
        b.date.setText(Formats.longDay(now.toLocalDate()) + " · " + Formats.time(now.toLocalTime()));

        int bookingReq = repo.pendingRequests(BookingRequest.Type.BOOKING).size();
        int cancelReq = repo.pendingRequests(BookingRequest.Type.CANCELLATION).size();
        int total = bookingReq + cancelReq;
        b.requestsBanner.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        b.allClear.setVisibility(total > 0 ? View.GONE : View.VISIBLE);
        b.bannerTitle.setText(getString(R.string.dash_requests_banner, total));
        b.bannerSub.setText(getString(R.string.dash_requests_banner_sub, bookingReq, cancelReq));

        int rooms = repo.getRooms(null).size();
        stat(b.stats.statVacant, R.drawable.ic_meeting_room, repo.vacantRoomCount(now) + " / " + rooms, getString(R.string.dash_vacant_now));
        stat(b.stats.statToday, R.drawable.ic_event, String.valueOf(repo.bookingsOn(now.toLocalDate()).size()), getString(R.string.dash_today_count));
        stat(b.stats.statRequests, R.drawable.ic_inbox, String.valueOf(bookingReq), getString(R.string.dash_booking_requests));
        stat(b.stats.statCancellations, R.drawable.ic_cancel, String.valueOf(cancelReq), getString(R.string.dash_cancel_requests));

        adapter.submit(repo.bookingsOn(now.toLocalDate()), false);
    }

    private void stat(ViewStatCardBinding card, @DrawableRes int icon, String value, String label) {
        card.statIcon.setImageResource(icon);
        card.statValue.setText(value);
        card.statLabel.setText(label);
        card.getRoot().setContentDescription(value + " " + label);
    }

    private void openRequests(int tab) {
        Bundle args = new Bundle();
        args.putInt("tab", tab);
        Nav.toTopLevel(this, R.id.nav_requests, args);
    }

    private static Bundle bookingArgs(String id) {
        Bundle args = new Bundle();
        args.putString("bookingId", id);
        return args;
    }

    private void navigate(int dest, @Nullable Bundle args) {
        NavHostFragment.findNavController(this).navigate(dest, args);
    }

    @Override
    public void onDestroyView() {
        RoomHubRepository.get().removeListener(refresher);
        b = null;
        super.onDestroyView();
    }
}
