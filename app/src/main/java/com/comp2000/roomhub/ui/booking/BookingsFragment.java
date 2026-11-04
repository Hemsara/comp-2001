package com.comp2000.roomhub.ui.booking;

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
import com.comp2000.roomhub.data.RoomHubRepository.BookingFilter;
import com.comp2000.roomhub.data.Session;
import com.comp2000.roomhub.data.model.Booking;
import com.comp2000.roomhub.data.model.User;
import com.comp2000.roomhub.databinding.FragmentBookingsBinding;
import com.comp2000.roomhub.ui.common.Nav;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/** Every booking in the workplace, grouped by day, with tabs for upcoming, past and cancelled. */
public class BookingsFragment extends Fragment {
    private FragmentBookingsBinding b;
    private BookingAdapter adapter;
    private int tab;
    private User personFilter;
    private final Runnable refresher = this::render;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentBookingsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        adapter = new BookingAdapter(bk -> Nav.to(this, R.id.nav_booking_detail, Nav.args("bookingId", bk.id)));
        int span = getResources().getInteger(R.integer.list_grid_span);
        GridLayoutManager lm = new GridLayoutManager(requireContext(), span);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? span : 1;
            }
        });
        b.list.setLayoutManager(lm);
        b.list.setAdapter(adapter);

        for (int id : new int[]{R.string.bookings_upcoming, R.string.bookings_past, R.string.bookings_cancelled}) {
            b.tabs.addTab(b.tabs.newTab().setText(id));
        }
        TabLayout.Tab selected = b.tabs.getTabAt(tab);
        if (selected != null) selected.select();
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

        b.personFilter.setOnClickListener(v -> choosePerson());
        b.mineFilter.setOnCheckedChangeListener((c, checked) -> {
            personFilter = checked ? Session.user() : null;
            render();
        });
        RoomHubRepository.get().addListener(refresher);
        render();
    }

    private void choosePerson() {
        List<User> users = RoomHubRepository.get().getAllUsers();
        String[] names = new String[users.size() + 1];
        names[0] = getString(R.string.bookings_everyone);
        for (int i = 0; i < users.size(); i++) names[i + 1] = users.get(i).name;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.bookings_filter_person)
                .setItems(names, (d, which) -> {
                    personFilter = which == 0 ? null : users.get(which - 1);
                    b.mineFilter.setChecked(personFilter != null && personFilter.id.equals(Session.user().id));
                    render();
                })
                .show();
    }

    private void render() {
        if (b == null) return;
        BookingFilter filter = BookingFilter.values()[tab];
        List<Booking> list = RoomHubRepository.get().getBookings(filter, personFilter);
        adapter.submit(list, true);
        b.personFilter.setText(personFilter == null ? getString(R.string.bookings_everyone) : personFilter.name);
        b.empty.getRoot().setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        b.empty.emptyIcon.setImageResource(R.drawable.ic_event);
        b.empty.emptyText.setText(R.string.bookings_empty);
    }

    @Override
    public void onDestroyView() {
        RoomHubRepository.get().removeListener(refresher);
        b = null;
        super.onDestroyView();
    }
}
