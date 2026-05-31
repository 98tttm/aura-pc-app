package com.example.aura_pc_app.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiResponse;
import com.example.aura_pc_app.databinding.FragmentOtpVerificationBinding;
import com.example.aura_pc_app.ui.base.BaseFragment;

public class OtpVerificationFragment extends BaseFragment<FragmentOtpVerificationBinding> {

    private AuthViewModel viewModel;
    private boolean resendRequested;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo AuthViewModel chung
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        setupViews();
        setupListeners();
        observeViewModel();
    }

    private void setupViews() {
        // Hiển thị số điện thoại nhận mã trong Subtitle
        String phoneVal = viewModel.getPhone().getValue();
        binding.tvSubtitle.setText(getString(R.string.otp_subtitle, phoneVal));
    }

    private void setupListeners() {
        // Lắng nghe thay đổi OTP
        binding.etOtp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvOtpError.setVisibility(View.GONE);
                viewModel.getOtp().setValue(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Click nút "Xác nhận"
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

        // Click nút "Gửi lại OTP" khi đếm ngược kết thúc
        binding.btnResend.setOnClickListener(v -> {
            resendRequested = true;
            viewModel.requestOtp();
        });

        // Click link "Thay đổi số điện thoại" quay lại màn hình nhập SĐT
        binding.tvChangePhone.setOnClickListener(v -> {
            viewModel.stopCountdownTimer();
            viewModel.getOtp().setValue("");
            viewModel.clearRequestOtpResponse();
            viewModel.clearVerifyOtpResponse();
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).navigateToPhoneInput();
            }
        });

        // Hỗ trợ & Điều khoản
        binding.tvSupport.setOnClickListener(v -> 
            Toast.makeText(getContext(), R.string.msg_support_connecting, Toast.LENGTH_SHORT).show()
        );
        binding.tvTerms.setOnClickListener(v -> 
            Toast.makeText(getContext(), R.string.msg_terms_policy, Toast.LENGTH_SHORT).show()
        );
    }

    private void observeViewModel() {
        // Quan sát text đếm ngược countdown
        viewModel.getCountdownText().observe(getViewLifecycleOwner(), text -> {
            binding.tvCountdown.setText(getString(R.string.otp_countdown_text, text));
        });

        // Quan sát trạng thái bộ đếm chạy để ẩn/hiện nút Gửi lại mã
        viewModel.getIsTimerRunning().observe(getViewLifecycleOwner(), isRunning -> {
            if (isRunning) {
                binding.tvCountdown.setVisibility(View.VISIBLE);
                binding.btnResend.setVisibility(View.GONE);
            } else {
                binding.tvCountdown.setVisibility(View.GONE);
                binding.btnResend.setVisibility(View.VISIBLE);
            }
        });

        // Quan sát phản hồi API gửi lại OTP
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

        // Quan sát phản hồi API xác thực OTP
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

    @Override
    protected FragmentOtpVerificationBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentOtpVerificationBinding.inflate(inflater, container, false);
    }
}
