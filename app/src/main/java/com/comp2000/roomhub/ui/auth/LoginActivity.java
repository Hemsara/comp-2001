package com.comp2000.roomhub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.comp2000.roomhub.R;
import com.comp2000.roomhub.data.RoomHubRepository;
import com.comp2000.roomhub.data.Session;
import com.comp2000.roomhub.data.model.Role;
import com.comp2000.roomhub.data.model.User;
import com.comp2000.roomhub.databinding.ActivityLoginBinding;
import com.comp2000.roomhub.databinding.ViewLoginFormBinding;
import com.comp2000.roomhub.ui.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Authentication with inline validation, loading state and role-based access check. */
public class LoginActivity extends AppCompatActivity {
    private ViewLoginFormBinding form;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        form = binding.form;
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        form.signIn.setOnClickListener(v -> attemptSignIn());
        form.password.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSignIn();
                return true;
            }
            return false;
        });
        form.forgot.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.login_forgot)
                .setMessage(R.string.login_forgot_message)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }

    private void attemptSignIn() {
        form.errorBanner.setVisibility(View.GONE);
        form.emailLayout.setError(null);
        form.passwordLayout.setError(null);
        String email = String.valueOf(form.email.getText()).trim();
        String password = String.valueOf(form.password.getText());

        boolean ok = true;
        if (TextUtils.isEmpty(email)) {
            form.emailLayout.setError(getString(R.string.login_error_email_empty));
            ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            form.emailLayout.setError(getString(R.string.login_error_email_format));
            ok = false;
        }
        if (TextUtils.isEmpty(password)) {
            form.passwordLayout.setError(getString(R.string.login_error_password_empty));
            ok = false;
        }
        if (!ok) return;

        setLoading(true);
        // Simulated network latency so the loading state is visible.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setLoading(false);
            User user = RoomHubRepository.get().authenticate(email, password);
            if (user == null) {
                showError(getString(R.string.login_error_credentials));
                form.password.setText(null);
                form.password.requestFocus();
            } else if (user.role != Role.MANAGER) {
                showError(getString(R.string.login_error_role, user.name));
            } else {
                Session.signIn(user);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }, 900);
    }

    private void showError(String message) {
        form.errorText.setText(message);
        form.errorBanner.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        form.signIn.setEnabled(!loading);
        form.signIn.setText(loading ? "" : getString(R.string.login_button));
        form.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        form.email.setEnabled(!loading);
        form.password.setEnabled(!loading);
    }
}
