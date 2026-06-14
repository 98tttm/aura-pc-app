package com.example.aura_pc_app.ui.auth;

import android.app.Application;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aura_pc_app.data.cart.CartRepositoryImpl;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiResponse;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.data.repository.AuthRepositoryImpl;
import com.example.aura_pc_app.domain.repository.AuthRepository;
import com.example.aura_pc_app.domain.cart.CartRepository;
import com.example.aura_pc_app.ui.base.BaseViewModel;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuthViewModel extends BaseViewModel {

    private static final Gson GSON = new Gson();
    private static final Type USER_MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();
    private AuthRepository authRepository;
    
    // LiveData states
    private final MutableLiveData<String> phone = new MutableLiveData<>("");
    private final MutableLiveData<String> otp = new MutableLiveData<>("");
    
    private final MutableLiveData<String> countdownText = new MutableLiveData<>("05:00");
    private final MutableLiveData<Boolean> isTimerRunning = new MutableLiveData<>(false);
    
    private final MutableLiveData<ApiResponse<Map<String, Object>>> requestOtpResponse = new MutableLiveData<>();
    private final MutableLiveData<ApiResponse<Map<String, Object>>> verifyOtpResponse = new MutableLiveData<>();

    private CountDownTimer countDownTimer;

    public AuthViewModel(@NonNull Application application) {
        super(application);
    }

    // ── Getters / Setters ─────────────────────────────────

    public MutableLiveData<String> getPhone() {
        return phone;
    }

    public MutableLiveData<String> getOtp() {
        return otp;
    }

    public LiveData<String> getCountdownText() {
        return countdownText;
    }

    public LiveData<Boolean> getIsTimerRunning() {
        return isTimerRunning;
    }

    public LiveData<ApiResponse<Map<String, Object>>> getRequestOtpResponse() {
        return requestOtpResponse;
    }

    public LiveData<ApiResponse<Map<String, Object>>> getVerifyOtpResponse() {
        return verifyOtpResponse;
    }

    public void clearRequestOtpResponse() {
        requestOtpResponse.setValue(null);
    }

    public void clearVerifyOtpResponse() {
        verifyOtpResponse.setValue(null);
    }

    // ── Timer Logic ───────────────────────────────────────

    public void startCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        isTimerRunning.setValue(true);
        // 5 minutes = 300,000 milliseconds
        countDownTimer = new CountDownTimer(300000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                countdownText.setValue(String.format(Locale.US, "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                countdownText.setValue("00:00");
                isTimerRunning.setValue(false);
            }
        }.start();
    }

    public void stopCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning.setValue(false);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopCountdownTimer();
    }

    // ── Auth Actions ──────────────────────────────────────

    private AuthRepository getAuthRepository() {
        if (authRepository == null) {
            authRepository = new AuthRepositoryImpl(getApplication());
        }
        return authRepository;
    }

    public boolean validatePhoneNumber(String phoneNumber) {
        return normalizePhoneNumber(phoneNumber) != null;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.matches("^0[0-9]{9}$")) {
            return digits;
        }
        if (digits.matches("^84[0-9]{9}$")) {
            return "0" + digits.substring(2);
        }
        if (digits.matches("^[1-9][0-9]{8}$")) {
            return "0" + digits;
        }
        return null;
    }

    public boolean validateOtp(String otpCode) {
        if (otpCode == null) return false;
        return otpCode.matches("^[0-9]{6}$");
    }

    public void requestOtp() {
        String phoneStr = normalizePhoneNumber(phone.getValue());
        if (phoneStr == null) {
            requestOtpResponse.setValue(ApiResponse.error(localizedString(R.string.error_invalid_phone)));
            return;
        }

        phone.setValue(phoneStr);
        requestOtpResponse.setValue(ApiResponse.loading());
        // Delegate call to repository and pipe response
        getAuthRepository().requestOtp(phoneStr).observeForever(response -> {
            if (response != null) {
                requestOtpResponse.setValue(response);
                if (response.getStatus() == ApiResponse.Status.SUCCESS) {
                    startCountdownTimer();
                }
            }
        });
    }

    public void verifyOtp() {
        String phoneStr = normalizePhoneNumber(phone.getValue());
        String otpStr = otp.getValue();
        if (phoneStr == null || !validateOtp(otpStr)) {
            verifyOtpResponse.setValue(ApiResponse.error(localizedString(R.string.error_invalid_otp)));
            return;
        }

        phone.setValue(phoneStr);
        verifyOtpResponse.setValue(ApiResponse.loading());
        getAuthRepository().verifyOtp(phoneStr, otpStr).observeForever(response -> {
            if (response != null) {
                verifyOtpResponse.setValue(response);
                if (response.getStatus() == ApiResponse.Status.SUCCESS && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    String accessToken = getStringValue(data, "token", "accessToken", "access_token");
                    String refreshToken = getStringValue(data, "refreshToken", "refresh_token");
                    TokenManager tokenManager = TokenManager.getInstance(getApplication());

                    if (accessToken != null) {
                        tokenManager.saveAccessToken(accessToken);
                    }
                    if (refreshToken != null) {
                        tokenManager.saveRefreshToken(refreshToken);
                    }
                    Object user = data.get("user");
                    if (user != null) {
                        tokenManager.saveCurrentUserJson(GSON.toJson(normalizeUserForSession(user, phoneStr)));
                    }
                    new CartRepositoryImpl(getApplication()).syncCartAfterLogin(new CartRepository.CartCallback() {
                        @Override
                        public void onSuccess() {
                            // Cart badge and cart screen observe Room, so no direct UI event is needed here.
                        }

                        @Override
                        public void onError(String message) {
                            postError(message);
                        }
                    });
                }
            }
        });
    }

    private Map<String, Object> normalizeUserForSession(Object rawUser, String fallbackPhone) {
        Map<String, Object> user = GSON.fromJson(GSON.toJson(rawUser), USER_MAP_TYPE);
        if (user == null) {
            user = new HashMap<>();
        } else {
            user = new HashMap<>(user);
        }

        String normalizedPhone = normalizePhoneNumber(firstString(user, "phone", "phoneNumber", "phone_number", "mobile", "username"));
        if (normalizedPhone == null) {
            normalizedPhone = normalizePhoneNumber(fallbackPhone);
        }
        if (normalizedPhone != null) {
            user.put("phoneNumber", normalizedPhone);
            normalizePhoneField(user, "phone");
            normalizePhoneField(user, "phone_number");
            normalizePhoneField(user, "mobile");
            normalizePhoneField(user, "username");
        }
        return user;
    }

    private void normalizePhoneField(Map<String, Object> user, String key) {
        Object value = user.get(key);
        if (value == null) {
            return;
        }
        String normalized = normalizePhoneNumber(String.valueOf(value));
        if (normalized != null) {
            user.put(key, normalized);
        }
    }

    private static String getStringValue(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof String && !((String) value).isEmpty()) {
                return (String) value;
            }
        }
        return null;
    }

    private static String firstString(Map<String, Object> data, String... keys) {
        if (data == null) return "";
        for (String key : keys) {
            Object value = data.get(key);
            if (value == null) continue;
            String text = String.valueOf(value).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return "";
    }

    private String localizedString(int resId) {
        return LocaleManager.wrap(getApplication()).getString(resId);
    }
}
