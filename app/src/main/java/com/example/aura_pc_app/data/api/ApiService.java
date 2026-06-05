package com.example.aura_pc_app.data.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.PUT;

public interface ApiService {
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @POST("auth/request-otp")
    Call<Map<String, Object>> requestOtp(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<Map<String, Object>> verifyOtp(@Body Map<String, String> body);

    @GET("products")
    Call<Map<String, Object>> getProducts();

    @GET("users/{id}")
    Call<Map<String, Object>> getUserById(@Path("id") int id);

    @GET("users/me")
    Call<Map<String, Object>> getCurrentUser();

    @PATCH("users/me")
    Call<Map<String, Object>> updateCurrentUser(@Body Map<String, Object> profile);

    @PUT("auth/profile")
    Call<Map<String, Object>> updateCurrentUserProfile(@Body Map<String, Object> profile);

    @GET("products")
    Call<ProductResponse> getProductsPaginated(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @POST("cart/sync")
    Call<CartResponse> syncCartToServer(@Body SyncCartRequest request);
}
