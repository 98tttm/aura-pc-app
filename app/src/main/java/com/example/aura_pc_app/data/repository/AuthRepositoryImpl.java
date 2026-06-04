package com.example.aura_pc_app.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiResponse;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.domain.repository.AuthRepository;
import com.example.aura_pc_app.utils.Constants;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthRepositoryImpl implements AuthRepository {
    private final Application application;
    private static ApiService authApiService;
    private static final Gson GSON = new GsonBuilder().create();

    public AuthRepositoryImpl(Application application) {
        this.application = application;
    }

    private static synchronized ApiService getAuthApiService() {
        if (authApiService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .client(new OkHttpClient.Builder().build())
                    .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
                    .build();
            authApiService = retrofit.create(ApiService.class);
        }
        return authApiService;
    }

    @Override
    public LiveData<ApiResponse<Map<String, Object>>> requestOtp(String phone) {
        final MutableLiveData<ApiResponse<Map<String, Object>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResponse.loading());

        Map<String, String> body = new HashMap<>();
        body.put("phoneNumber", phone);

        getAuthApiService().requestOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Map<String, Object> responseBody = response.body();
                if (response.isSuccessful() && isBackendSuccess(responseBody)) {
                    liveData.setValue(ApiResponse.success(responseBody));
                } else {
                    liveData.setValue(ApiResponse.error(getBackendMessage(
                            response,
                            responseBody,
                            localizedString(R.string.error_request_otp_failed)
                    )));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                liveData.setValue(ApiResponse.error(localizedString(R.string.error_network_retry)));
            }
        });

        return liveData;
    }

    @Override
    public LiveData<ApiResponse<Map<String, Object>>> verifyOtp(String phone, String otp) {
        final MutableLiveData<ApiResponse<Map<String, Object>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResponse.loading());

        Map<String, String> body = new HashMap<>();
        body.put("phoneNumber", phone);
        body.put("otp", otp);

        getAuthApiService().verifyOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Map<String, Object> responseBody = response.body();
                if (response.isSuccessful() && isBackendSuccess(responseBody)) {
                    liveData.setValue(ApiResponse.success(responseBody));
                } else {
                    liveData.setValue(ApiResponse.error(getBackendMessage(
                            response,
                            responseBody,
                            localizedString(R.string.error_verify_otp_failed)
                    )));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                liveData.setValue(ApiResponse.error(localizedString(R.string.error_network_retry)));
            }
        });

        return liveData;
    }

    private String localizedString(int resId) {
        return LocaleManager.wrap(application).getString(resId);
    }

    private static boolean isBackendSuccess(Map<String, Object> body) {
        if (body == null) {
            return false;
        }
        Object success = body.get("success");
        return success == null || Boolean.TRUE.equals(success);
    }

    private static String getBackendMessage(Response<?> response, Map<String, Object> body, String fallback) {
        Object bodyMessage = body == null ? null : body.get("message");
        if (bodyMessage instanceof String && !((String) bodyMessage).isEmpty()) {
            return (String) bodyMessage;
        }

        try {
            if (response.errorBody() != null) {
                String rawError = response.errorBody().string();
                Map<?, ?> errorMap = GSON.fromJson(rawError, Map.class);
                Object errorMessage = errorMap == null ? null : errorMap.get("message");
                if (errorMessage instanceof String && !((String) errorMessage).isEmpty()) {
                    return (String) errorMessage;
                }
                Object error = errorMap == null ? null : errorMap.get("error");
                if (error instanceof String && !((String) error).isEmpty()) {
                    return (String) error;
                }
            }
        } catch (Exception ignored) {
            // Fall back to the local generic message when the server body is not JSON.
        }

        return fallback;
    }
}
