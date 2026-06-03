package com.example.aura_pc_app.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.databinding.FragmentOtpVerificationBinding;
import com.example.aura_pc_app.ui.base.BaseFragment;
import com.example.aura_pc_app.utils.LocaleManager;

public class OtpVerificationFragment extends BaseFragment<FragmentOtpVerificationBinding> {

    private AuthViewModel viewModel;
    private boolean resendRequested;
    private TextView[] otpDigits;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        setupViews();
        setupListeners();
        observeViewModel();
    }

    private void setupViews() {
        otpDigits = new TextView[] {
                binding.otpDigit1,
                binding.otpDigit2,
                binding.otpDigit3,
                binding.otpDigit4,
                binding.otpDigit5,
                binding.otpDigit6
        };
        updateOtpBoxes("");
        updateSubtitle(viewModel.getCountdownText().getValue());
    }

    private void setupListeners() {
        binding.etOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvOtpError.setVisibility(View.GONE);
                String otp = s.toString().trim();
                viewModel.getOtp().setValue(otp);
                updateOtpBoxes(otp);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etOtp.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.otpCard.animate()
                        .translationY(-dpToPx(150))
                        .setDuration(220L)
                        .start();
                scrollOtpIntoView();
            } else {
                binding.otpCard.animate()
                        .translationY(0f)
                        .setDuration(180L)
                        .start();
            }
        });
        binding.otpInputFrame.setOnClickListener(v -> binding.etOtp.requestFocus());
        binding.btnVerify.setOnClickListener(v -> {
            String otpVal = binding.etOtp.getText().toString().trim();
            if (viewModel.validateOtp(otpVal)) {
                binding.tvOtpError.setVisibility(View.GONE);
                viewModel.verifyOtp();
            } else {
                binding.tvOtpError.setText(R.string.error_invalid_otp);
                binding.tvOtpError.setVisibility(View.VISIBLE);
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).showTopNotification(
                            getString(R.string.notification_verify_failed_title),
                            getString(R.string.error_invalid_otp)
                    );
                }
            }
        });

        binding.btnResend.setOnClickListener(v -> {
            resendRequested = true;
            viewModel.requestOtp();
        });

        binding.tvChangePhone.setOnClickListener(v -> {
            viewModel.stopCountdownTimer();
            viewModel.getOtp().setValue("");
            viewModel.clearRequestOtpResponse();
            viewModel.clearVerifyOtpResponse();
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).navigateToPhoneInput();
            }
        });

        binding.tvSupport.setOnClickListener(v ->
                Toast.makeText(getContext(), R.string.msg_support_connecting, Toast.LENGTH_SHORT).show()
        );
        binding.tvTerms.setOnClickListener(v ->
                Toast.makeText(getContext(), R.string.msg_terms_policy, Toast.LENGTH_SHORT).show()
        );
    }

    private void observeViewModel() {
        viewModel.getCountdownText().observe(getViewLifecycleOwner(), this::updateSubtitle);

        viewModel.getIsTimerRunning().observe(getViewLifecycleOwner(), isRunning -> {
            binding.tvCountdown.setVisibility(View.GONE);
            binding.btnResend.setVisibility(View.VISIBLE);
        });

        viewModel.getRequestOtpResponse().observe(getViewLifecycleOwner(), response -> {
            if (response == null || !resendRequested) return;

            switch (response.getStatus()) {
                case LOADING:
                    binding.btnResend.setEnabled(false);
                    break;
                case SUCCESS:
                    resendRequested = false;
                    binding.btnResend.setEnabled(true);
                    binding.tvOtpError.setVisibility(View.GONE);
                    if (getActivity() instanceof AuthActivity) {
                        ((AuthActivity) getActivity()).showOtpNotification(
                                response.getData(),
                                viewModel.getPhone().getValue()
                        );
                    }
                    viewModel.clearRequestOtpResponse();
                    break;
                case ERROR:
                    resendRequested = false;
                    binding.btnResend.setEnabled(true);
                    binding.tvOtpError.setText(response.getMessage());
                    binding.tvOtpError.setVisibility(View.VISIBLE);
                    if (getActivity() instanceof AuthActivity) {
                        ((AuthActivity) getActivity()).showTopNotification(
                                getString(R.string.notification_request_failed_title),
                                response.getMessage()
                        );
                    }
                    viewModel.clearRequestOtpResponse();
                    break;
            }
        });

        viewModel.getVerifyOtpResponse().observe(getViewLifecycleOwner(), response -> {
            if (response == null) return;

            switch (response.getStatus()) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    if (getActivity() instanceof AuthActivity) {
                        AuthActivity activity = (AuthActivity) getActivity();
                        activity.showTopNotification(
                                getString(R.string.notification_verify_success_title),
                                getString(R.string.notification_verify_success_message)
                        );
                        binding.getRoot().postDelayed(activity::navigateToHome, 700L);
                    }
                    viewModel.clearVerifyOtpResponse();
                    break;
                case ERROR:
                    setLoadingState(false);
                    binding.tvOtpError.setText(response.getMessage());
                    binding.tvOtpError.setVisibility(View.VISIBLE);
                    if (getActivity() instanceof AuthActivity) {
                        ((AuthActivity) getActivity()).showTopNotification(
                                getString(R.string.notification_verify_failed_title),
                                response.getMessage()
                        );
                    }
                    viewModel.clearVerifyOtpResponse();
                    break;
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            binding.pbLoading.setVisibility(View.VISIBLE);
            binding.btnContent.setVisibility(View.GONE);
            binding.btnVerify.setClickable(false);
        } else {
            binding.pbLoading.setVisibility(View.GONE);
            binding.btnContent.setVisibility(View.VISIBLE);
            binding.btnVerify.setClickable(true);
        }
    }

    private void updateOtpBoxes(String otp) {
        if (otpDigits == null) return;
        for (int i = 0; i < otpDigits.length; i++) {
            otpDigits[i].setText(i < otp.length() ? String.valueOf(otp.charAt(i)) : "");
        }
    }

    private void updateSubtitle(String countdown) {
        String phoneVal = viewModel.getPhone().getValue();
        binding.tvSubtitle.setText(getString(
                R.string.otp_subtitle_countdown,
                phoneVal,
                formatCountdown(countdown)
        ));
    }

    private String formatCountdown(String countdown) {
        if (countdown == null || !countdown.contains(":")) {
            return LocaleManager.isVietnamese(requireContext()) ? "5 phút" : "5 min";
        }

        String[] parts = countdown.split(":");
        if (parts.length != 2) return countdown;

        try {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            if (LocaleManager.isVietnamese(requireContext())) {
                return minutes + " phút " + seconds + " giây";
            }
            return minutes + " min " + seconds + " sec";
        } catch (NumberFormatException ignored) {
            return countdown;
        }
    }

    private void scrollOtpIntoView() {
        binding.authScroll.postDelayed(() -> {
            int[] scrollLocation = new int[2];
            int[] inputLocation = new int[2];
            binding.authScroll.getLocationOnScreen(scrollLocation);
            binding.otpInputFrame.getLocationOnScreen(inputLocation);

            int inputTopInScroll = inputLocation[1] - scrollLocation[1] + binding.authScroll.getScrollY();
            int targetScroll = Math.max(0, inputTopInScroll - dpToPx(170));
            binding.authScroll.smoothScrollTo(0, targetScroll);
        }, 260L);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected FragmentOtpVerificationBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentOtpVerificationBinding.inflate(inflater, container, false);
    }
}
