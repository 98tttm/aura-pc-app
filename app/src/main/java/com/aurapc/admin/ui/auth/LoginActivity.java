package com.aurapc.admin.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.api.AuthApi;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.local.TokenManager;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.ui.main.MainActivity;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private CheckBox cbRemember;
    private MaterialButton btnLogin;
    private ProgressBar progress;
    private TextView tvError;
    private LinearLayout biometricGroup;
    private ImageButton btnBiometric;

    private TokenManager tokenManager;
    private AuthApi authApi;
    private final MutableLiveData<Boolean> _loginState = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = ServiceLocator.get().tokenManager();
        authApi = ServiceLocator.get().apiClient().authApi();

        if (tokenManager.isLoggedIn() && tokenManager.isBiometricEnabled()) {
            navigateToMain();
            return;
        }

        initViews();
        setupBiometric();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRemember = findViewById(R.id.cbRemember);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);
        biometricGroup = findViewById(R.id.biometricGroup);

        // Pre-fill if remembered
        String savedEmail = tokenManager.getAdminEmail();
        if (savedEmail != null && !savedEmail.isEmpty()) {
            etEmail.setText(savedEmail);
            cbRemember.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        biometricGroup.setOnClickListener(v -> authenticateWithBiometric());
    }

    private void setupBiometric() {
        BiometricManager bm = BiometricManager.from(this);
        boolean canAuth = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS;

        if (canAuth && tokenManager.isBiometricEnabled() && tokenManager.isLoggedIn()) {
            biometricGroup.setVisibility(View.VISIBLE);
            biometricGroup.setOnClickListener(v -> authenticateWithBiometric());
        } else {
            biometricGroup.setVisibility(View.GONE);
        }
    }

    private void authenticateWithBiometric() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        navigateToMain();
                    }

                    @Override
                    public void onAuthenticationError(int errCode, CharSequence errString) {
                        if (errCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Snackbar.make(LoginActivity.this, findViewById(android.R.id.content),
                                    "Xác thực thất bại: " + errString, Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Đăng nhập AuraPC Admin")
                .setSubtitle("Xác minh danh tính quản trị viên")
                .setNegativeButtonText("Hủy")
                .build();

        prompt.authenticate(info);
    }

    private void attemptLogin() {
        try {
            tilEmail.setError(null);
            tilPassword.setError(null);
            tvError.setVisibility(View.GONE);

            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

            if (email.isEmpty()) {
                tilEmail.setError("Email là bắt buộc");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Email không hợp lệ");
                return;
            }
            if (password.isEmpty()) {
                tilPassword.setError("Mật khẩu là bắt buộc");
                return;
            }

            setLoading(true);

            LiveData<Resource<AuthApi.LoginResponse>> call = NetworkHelper.toLiveData(
                    authApi.login(new AuthApi.LoginRequest(email, password))
            );
            call.observe(this, result -> {
                try {
                    if (result == null) {
                        setLoading(false);
                        tvError.setText(getString(R.string.error_generic));
                        tvError.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (result.isLoading()) {
                        return;
                    }
                    setLoading(false);
                    if (result.isSuccess()) {
                        AuthApi.LoginResponse lr = result.data;
                        if (lr == null || lr.token == null) {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + (lr != null && lr.message != null ? lr.message : "Vui lòng thử lại"), Toast.LENGTH_LONG).show();
                            return;
                        }
                        tokenManager.saveToken(lr.token);
                        AuthApi.AdminUser a = lr.admin;
                        if (a != null) {
                            tokenManager.saveAdminProfile(
                                    a._id != null ? a._id : a.id,
                                    a.email,
                                    a.name,
                                    a.role,
                                    a.avatar
                            );
                        }
                        navigateToMain();
                    } else {
                        tvError.setText(result.message != null ? result.message : getString(R.string.error_generic));
                        tvError.setVisibility(View.VISIBLE);
                    }
                } catch (Throwable t) {
                    setLoading(false);
                    android.util.Log.e("LoginActivity", "callback crash", t);
                    Toast.makeText(LoginActivity.this, "Lỗi xử lý: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Throwable t) {
            setLoading(false);
            android.util.Log.e("LoginActivity", "attemptLogin crash", t);
            Toast.makeText(this, "Lỗi: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
