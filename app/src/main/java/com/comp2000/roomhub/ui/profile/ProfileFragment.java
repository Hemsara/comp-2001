package com.comp2000.roomhub.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.Session;
import com.comp2000.roomhub.data.model.User;
import com.comp2000.roomhub.databinding.FragmentProfileBinding;
import com.comp2000.roomhub.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Signed-in identity, visible role and sign-out. */
public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        User u = Session.user();
        b.avatar.setText(u.initials());
        b.avatar.setContentDescription(getString(R.string.cd_avatar, u.name));
        b.name.setText(u.name);
        b.email.setText(u.email);
        b.role.setText(getString(R.string.profile_role_manager) + " · " + u.department);
        b.help.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_help)
                .setMessage("RoomHub follows your device text size and dark theme settings. Status is always shown with text and an icon, never colour alone.")
                .setPositiveButton(android.R.string.ok, null)
                .show());
        b.signOut.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_signout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.profile_signout, (d, w) -> {
                    Session.signOut();
                    Intent i = new Intent(requireContext(), LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .show());
    }

    @Override
    public void onDestroyView() {
        b = null;
        super.onDestroyView();
    }
}
