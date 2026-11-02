package com.comp2000.roomhub.ui.common;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

/** Navigation helpers so shortcuts behave exactly like tapping the navigation bar. */
public final class Nav {
    private Nav() {
    }

    public static void toTopLevel(Fragment from, int destination, @Nullable Bundle args) {
        NavController nc = NavHostFragment.findNavController(from);
        NavOptions opts = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(nc.getGraph().getStartDestinationId(), false)
                .build();
        nc.navigate(destination, args, opts);
    }

    public static void to(Fragment from, int destination, @Nullable Bundle args) {
        NavHostFragment.findNavController(from).navigate(destination, args);
    }

    public static Bundle args(String key, String value) {
        Bundle b = new Bundle();
        b.putString(key, value);
        return b;
    }
}
