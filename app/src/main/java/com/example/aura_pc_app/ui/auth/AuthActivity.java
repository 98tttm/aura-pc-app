package com.example.aura_pc_app.ui.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.aura.pc.ui.profile.CheckingAccountActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.databinding.ActivityAuthBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

import java.util.Map;

public class AuthActivity extends BaseActivity<ActivityAuthBinding> {

    public static final String EXTRA_LOGIN_SUCCESS = "extra_login_success";
    private static final long NOTIFICATION_VISIBLE_MS = 7000L;

    private AuthViewModel viewModel;
    private Runnable hideNotificationRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra(EXTRA_LOGIN_SUCCESS, false)) {
            navigateAfterLoginSuccess(this);
            finish();
            return;
        }

        configureSystemBars();
        setupLanguageToggle();

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.auth_container, new PhoneInputFragment())
                    .commit();
        }
    }

    private void configureSystemBars() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(getColor(R.color.white));
        getWindow().setNavigationBarColor(getColor(R.color.white));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void setupLanguageToggle() {
        binding.btnLanguageToggle.setText(LocaleManager.isVietnamese(this)
                ? R.string.language_switch_to_en
                : R.string.language_switch_to_vi);
        binding.btnLanguageToggle.setOnClickListener(v -> {
            LocaleManager.toggleLanguage(this);
            recreate();
        });
        binding.btnCloseAuth.setOnClickListener(v -> finish());
    }

    public void showOtpNotification(Map<String, Object> data, String fallbackPhone) {
        String devOtp = getStringValue(data, "devOtp", "otp", "code");
        if (devOtp != null) {
            showTopNotification(
                    getString(R.string.notification_otp_title),
                    getString(R.string.notification_otp_message, devOtp)
            );
        } else {
            showTopNotification(
                    getString(R.string.notification_otp_title),
                    getString(R.string.notification_otp_sent, fallbackPhone)
            );
        }
    }

    public void showTopNotification(String title, String message) {
        if (binding == null) return;

        if (hideNotificationRunnable != null) {
            binding.authNotification.removeCallbacks(hideNotificationRunnable);
        }

        binding.tvNotificationTitle.setText(title);
        binding.tvNotificationMessage.setText(message);
        binding.authNotification.clearAnimation();
        binding.authNotification.setVisibility(View.VISIBLE);
        binding.authNotification.setTranslationY(-dpToPx(120));
        binding.authNotification.setAlpha(0f);
        binding.authNotification.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(260L)
                .start();

        hideNotificationRunnable = () -> {
            if (binding == null) return;
            binding.authNotification.animate()
                    .translationY(-binding.authNotification.getHeight() - dpToPx(32))
                    .alpha(0f)
                    .setDuration(220L)
                    .withEndAction(() -> {
                        if (binding != null) {
                            binding.authNotification.setVisibility(View.GONE);
                        }
                    })
                    .start();
        };
        binding.authNotification.postDelayed(hideNotificationRunnable, NOTIFICATION_VISIBLE_MS);
    }

    public void navigateToOtpVerification() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.auth_container, new OtpVerificationFragment())
                .addToBackStack(null)
                .commit();
    }

    public void navigateToPhoneInput() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_container, new PhoneInputFragment())
                .commit();
    }

    public void navigateToHome() {
        Intent intent = createPostLoginIntent();
        startActivity(intent);
        finish();
    }

    private Intent createPostLoginIntent() {
        Intent intent = new Intent(this, CheckingAccountActivity.class);
        String redirectClassName = getIntent().getStringExtra(AuthGate.EXTRA_REDIRECT_CLASS_NAME);
        if (redirectClassName != null && !redirectClassName.isEmpty()) {
            intent.putExtra(AuthGate.EXTRA_REDIRECT_CLASS_NAME, redirectClassName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    public static void navigateAfterLoginSuccess(Context context) {
        Intent intent = new Intent(context, CheckingAccountActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected ActivityAuthBinding inflateBinding() {
        return ActivityAuthBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onDestroy() {
        if (binding != null && hideNotificationRunnable != null) {
            binding.authNotification.removeCallbacks(hideNotificationRunnable);
        }
        super.onDestroy();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static String getStringValue(Map<String, Object> data, String... keys) {
        if (data == null) return null;
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof String && !((String) value).isEmpty()) {
                return (String) value;
            }
            if (value instanceof Number) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
