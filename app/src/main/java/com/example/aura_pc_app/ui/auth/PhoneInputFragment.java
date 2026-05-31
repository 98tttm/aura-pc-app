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
import com.example.aura_pc_app.databinding.FragmentPhoneInputBinding;
import com.example.aura_pc_app.ui.base.BaseFragment;

public class PhoneInputFragment extends BaseFragment<FragmentPhoneInputBinding> {

    private AuthViewModel viewModel;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo AuthViewModel (sử dụng Activity scope để chia sẻ dữ liệu)
        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        // Lắng nghe thay đổi số điện thoại trong ô nhập để xóa thông báo lỗi
        binding.etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvPhoneError.setVisibility(View.GONE);
                viewModel.getPhone().setValue(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Xử lý sự kiện click nút "Gửi mã OTP"
        binding.btnContinue.setOnClickListener(v -> {
            String phoneVal = binding.etPhone.getText().toString().trim();
            if (viewModel.validatePhoneNumber(phoneVal)) {
                binding.tvPhoneError.setVisibility(View.GONE);
                viewModel.requestOtp();
            } else {
                binding.tvPhoneError.setText(R.string.error_invalid_phone);
                binding.tvPhoneError.setVisibility(View.VISIBLE);
            }
        });

        // Thiết lập link hỗ trợ / điều khoản
        binding.tvSupport.setOnClickListener(v -> 
            Toast.makeText(getContext(), R.string.msg_support_connecting, Toast.LENGTH_SHORT).show()
        );
        binding.tvTerms.setOnClickListener(v -> 
            Toast.makeText(getContext(), R.string.msg_terms_policy, Toast.LENGTH_SHORT).show()
        );
        binding.btnGoogle.setOnClickListener(v -> 
            Toast.makeText(getContext(), R.string.msg_google_login_pending, Toast.LENGTH_SHORT).show()
        );
        binding.btnFacebook.setOnClickListener(v ->
            Toast.makeText(getContext(), R.string.msg_facebook_login_pending, Toast.LENGTH_SHORT).show()
        );
    }

    private void observeViewModel() {
        // Quan sát phản hồi từ API gửi OTP
        viewModel.getRequestOtpResponse().observe(getViewLifecycleOwner(), response -> {
            if (response == null) return;

            switch (response.getStatus()) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    if (getActivity() instanceof AuthActivity) {
                        ((AuthActivity) getActivity()).showOtpNotification(
                                response.getData(),
                                viewModel.getPhone().getValue()
                        );
                    }
                    // Chuyển sang màn hình xác thực OTP
                    if (getActivity() instanceof AuthActivity) {
                        viewModel.clearRequestOtpResponse();
                        ((AuthActivity) getActivity()).navigateToOtpVerification();
                    }
                    break;
                case ERROR:
                    setLoadingState(false);
                    binding.tvPhoneError.setText(response.getMessage());
                    binding.tvPhoneError.setVisibility(View.VISIBLE);
                    if (getActivity() instanceof AuthActivity) {
                        ((AuthActivity) getActivity()).showTopNotification(
                                getString(R.string.notification_request_failed_title),
                                response.getMessage()
                        );
                    }
                    break;
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            binding.pbLoading.setVisibility(View.VISIBLE);
            binding.btnContent.setVisibility(View.GONE);
            binding.btnContinue.setClickable(false);
        } else {
            binding.pbLoading.setVisibility(View.GONE);
            binding.btnContent.setVisibility(View.VISIBLE);
            binding.btnContinue.setClickable(true);
        }
    }

    @Override
    protected FragmentPhoneInputBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentPhoneInputBinding.inflate(inflater, container, false);
    }
}
