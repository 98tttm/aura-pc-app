package com.aurapc.admin.data.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface AuthApi {

    @POST("api/admin/auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("api/admin/auth/refresh")
    Call<LoginResponse> refreshToken(@Body Map<String, String> body);

    @POST("api/admin/auth/logout")
    Call<Map<String, Object>> logout();

    @GET("api/admin/admins/me")
    Call<MeResponse> me();

    @GET("api/admin/admins")
    Call<AdminListResponse> listAdmins();

    @POST("api/admin/admins")
    Call<MeResponse> createAdmin(@Body AdminUser body);

    @PUT("api/admin/admins/{id}")
    Call<MeResponse> updateAdmin(@Path("id") String id, @Body AdminUser body);

    class LoginRequest {
        public String email;
        public String password;
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    class LoginResponse {
        public boolean success;
        public String token;
        public AdminUser admin;
        public String message;
    }

    class MeResponse {
        public boolean success;
        public AdminUser admin;
        public String message;
    }

    class AdminListResponse {
        public List<AdminUser> admins;
        public List<AdminUser> items;
    }

    class AdminUser {
        public String _id;
        public String id;
        public String email;
        public String name;
        public String role;
        public String avatar;
        public Boolean isActive;
        public List<String> permissions;
        @SerializedName("createdAt")
        public String createdAt;
    }
}