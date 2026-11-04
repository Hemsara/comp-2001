package com.comp2000.roomhub.ui.requests;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.model.BookingRequest;
import com.comp2000.roomhub.databinding.FragmentRequestsBinding;
import com.comp2000.roomhub.ui.common.Nav;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/** CR3: booking and cancellation requests in one destination, with live counts. */
public class RequestsFragment extends Fragment implements RequestAdapter.Listener {
    private FragmentRequestsBinding b;
    private RequestAdapter adapter;
    private int tab = -1;
    private final Runnable refresher = this::render;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentRequestsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        if (tab < 0) tab = getArguments() != null ? getArguments().getInt("tab", 0) : 0;
        adapter = new RequestAdapter(this);
        b.list.setLayoutManager(new GridLayoutManager(requireContext(), getResources().getInteger(R.integer.list_grid_span)));
        b.list.setAdapter(adapter);
        b.tabs.addTab(b.tabs.newTab());
        b.tabs.addTab(b.tabs.newTab());
        TabLayout.Tab t0 = b.tabs.getTabAt(tab);
        if (t0 != null) t0.select();
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                tab = t.getPosition();
                render();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });
        RoomHubRepository.get().addListener(refresher);
        render();
    }

    private void render() {
        if (b == null) return;
        RoomHubRepository repo = RoomHubRepository.get();
        List<BookingRequest> bookings = repo.pendingRequests(BookingRequest.Type.BOOKING);
        List<BookingRequest> cancels = repo.pendingRequests(BookingRequest.Type.CANCELLATION);
        b.tabs.getTabAt(0).setText(getString(R.string.req_tab_bookings, bookings.size()));
        b.tabs.getTabAt(1).setText(getString(R.string.req_tab_cancellations, cancels.size()));
        List<BookingRequest> shown = tab == 0 ? bookings : cancels;
        adapter.submit(shown);
        b.empty.getRoot().setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        b.empty.emptyText.setText(R.string.req_empty);
    }

    @Override
    public void onOpen(BookingRequest r) {
        Nav.to(this, R.id.nav_request_detail, Nav.args("requestId", r.id));
    }

    @Override
    public void onApprove(BookingRequest r) {
        RequestActions.approve(this, r, () -> { });
    }

    @Override
    public void onDecline(BookingRequest r) {
        RequestActions.decline(this, r, () -> { });
    }

    @Override
    public void onDestroyView() {
        RoomHubRepository.get().removeListener(refresher);
        b = null;
        super.onDestroyView();
    }
}
