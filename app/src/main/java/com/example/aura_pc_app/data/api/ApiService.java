package com.example.aura_pc_app.data.api;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;

public interface ApiService {
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @GET("products")
    Call<List<Map<String, Object>>> getProducts();

    @GET("users/{id}")
    Call<Map<String, Object>> getUserById(@Path("id") int id);
}
