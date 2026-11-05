package com.comp2000.roomhub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.Session;
import com.comp2000.roomhub.ui.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;

/**
 * Single-activity shell. Uses a bottom navigation bar on compact widths and a navigation rail on
 * widths of 600dp and above (landscape phones and tablets) – see layout-w600dp/activity_main.xml.
 */
public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private NavigationBarView navBar;
    private final Runnable badgeUpdater = this::updateBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        if (Session.user() == null) {
            // Process was recreated without a session: authentication is always required.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        navBar = findViewById(R.id.nav_bar);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = host.getNavController();
        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_rooms, R.id.nav_bookings, R.id.nav_requests, R.id.nav_profile).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfig);
        NavigationUI.setupWithNavController(navBar, navController);

        boolean bottomBar = navBar instanceof BottomNavigationView;
        navController.addOnDestinationChangedListener((c, dest, args) -> {
            // Hide the bottom bar during the booking flow to keep focus on the form (rail stays visible).
            boolean focused = dest.getId() == R.id.nav_create_booking || dest.getId() == R.id.nav_booking_confirmed;
            if (bottomBar) navBar.setVisibility(focused ? View.GONE : View.VISIBLE);
        });

        if (navBar instanceof NavigationRailView) {
            View header = ((NavigationRailView) navBar).getHeaderView();
            if (header != null) {
                header.findViewById(R.id.rail_fab).setOnClickListener(v -> navController.navigate(R.id.nav_create_booking));
            }
        }

        applyInsets(bottomBar);
        RoomHubRepository.get().addListener(badgeUpdater);
        updateBadge();
    }

    private void applyInsets(boolean bottomBar) {
        View appBar = findViewById(R.id.app_bar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(bottomBar ? bars.left : 0, bars.top, bars.right, 0);
            if (bottomBar) {
                navBar.setPadding(bars.left, 0, bars.right, bars.bottom);
                findViewById(R.id.nav_host).setPadding(bars.left, 0, bars.right, 0);
            } else {
                navBar.setPadding(bars.left, bars.top, 0, bars.bottom);
                findViewById(R.id.nav_host).setPadding(0, 0, bars.right, bars.bottom);
            }
            return insets;
        });
    }

    /** Refinement CR3: pending request count is always visible on the Requests destination. */
    private void updateBadge() {
        int count = RoomHubRepository.get().pendingCount();
        BadgeDrawable badge = navBar.getOrCreateBadge(R.id.nav_requests);
        badge.setNumber(count);
        badge.setVisible(count > 0);
        navBar.getMenu().findItem(R.id.nav_requests)
                .setContentDescription(getString(R.string.nav_requests) + ", " + count + " pending");
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        navController.handleDeepLink(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        RoomHubRepository.get().removeListener(badgeUpdater);
        super.onDestroy();
    }
}
