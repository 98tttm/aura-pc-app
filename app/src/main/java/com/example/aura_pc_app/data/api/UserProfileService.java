package com.example.aura_pc_app.data.api;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

public class UserProfileService {

    private final ApiService apiService;

    public UserProfileService(Context context) {
        apiService = ApiClient.getInstance(context).getApiService();
    }

    public Call<Map<String, Object>> updateCurrentUserProfile(ProfileUpdateRequest request) {
        return apiService.updateCurrentUserProfile(request.toBackendPayload());
    }

    public static class ProfileUpdateRequest {
        private final String fullName;
        private final String email;
        private final String dateOfBirth;
        private final String gender;
        private final String phoneNumber;
        private final String address;

        public ProfileUpdateRequest(
                String fullName,
                String email,
                String dateOfBirth,
                String gender,
                String phoneNumber,
                String address
        ) {
            this.fullName = trimToNull(fullName);
            this.email = trimToNull(email);
            this.dateOfBirth = trimToNull(dateOfBirth);
            this.gender = trimToNull(gender);
            this.phoneNumber = trimToNull(phoneNumber);
            this.address = trimToNull(address);
        }

        private Map<String, Object> toBackendPayload() {
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> profile = new HashMap<>();
            profile.put("fullName", fullName);
            if (dateOfBirth != null) profile.put("dateOfBirth", dateOfBirth);
            if (gender != null) profile.put("gender", gender);
            body.put("profile", profile);

            if (email != null) body.put("email", email);
            if (phoneNumber != null) body.put("phoneNumber", phoneNumber);
            if (address != null) body.put("address", address);
            return body;
        }

        private static String trimToNull(String value) {
            if (value == null) return null;
            String trimmed = value.trim();
            return TextUtils.isEmpty(trimmed) ? null : trimmed;
        }
    }
}
