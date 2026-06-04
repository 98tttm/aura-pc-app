package com.example.aura_pc_app.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.aura_pc_app.data.api.ApiResponse;
import java.util.Map;

public interface AuthRepository {
    LiveData<ApiResponse<Map<String, Object>>> requestOtp(String phone);
    LiveData<ApiResponse<Map<String, Object>>> verifyOtp(String phone, String otp);
}
