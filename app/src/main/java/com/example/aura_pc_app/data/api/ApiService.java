package com.example.aura_pc_app.data.api;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query; // Thêm thư viện Query cho phân trang

public interface ApiService {
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @GET("products")
    Call<Map<String, Object>> getProducts();

    @GET("users/{id}")
    Call<Map<String, Object>> getUserById(@Path("id") int id);

    @GET("products")
    Call<ProductResponse> getProductsPaginated(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @POST("cart/sync")
    Call<CartResponse> syncCartToServer(@Body SyncCartRequest request);

    @POST("auth/request-otp")
    Call<Map<String, Object>> requestOtp(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<Map<String, Object>> verifyOtp(@Body Map<String, String> body);
}