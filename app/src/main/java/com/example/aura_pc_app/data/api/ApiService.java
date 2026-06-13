package com.example.aura_pc_app.data.api;

import com.aura.pc.ui.address.AddressListResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface ApiService {
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @POST("auth/request-otp")
    Call<Map<String, Object>> requestOtp(@Body Map<String, String> body);

    @POST("auth/verify-otp")
    Call<Map<String, Object>> verifyOtp(@Body Map<String, String> body);

    @GET("products")
    Call<Map<String, Object>> getProducts();

    @GET("products/{id}")
    Call<Map<String, Object>> getProductById(@Path("id") String id);

    @GET("products")
    Call<Map<String, Object>> getProductsPaginatedMap(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("products")
    Call<Map<String, Object>> getProductsFiltered(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("category") String category,
            @Query("brand") String brand,
            @Query("min_price") Double minPrice,
            @Query("max_price") Double maxPrice,
            @Query("min_rating") Double minRating,
            @Query("in_stock") Boolean inStock,
            @Query("sort") String sort,
            @Query("search") String search
    );

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

    @GET("products")
    Call<ProductResponse> searchProducts(
            @Query("search") String search,
            @Query("limit") int limit
    );

    @POST("cart/sync")
    Call<CartResponse> syncCartToServer(@Body SyncCartRequest request);

    @GET("auth/addresses/{userId}")
    Call<AddressListResponse> getAddresses(@Path("userId") String userId);

    @POST("auth/addresses")
    Call<AddressListResponse> addAddress(@Body Map<String, Object> body);

    @PUT("auth/addresses/{addressId}")
    Call<AddressListResponse> updateAddress(
            @Path("addressId") String addressId,
            @Body Map<String, Object> body
    );

    @HTTP(method = "DELETE", path = "auth/addresses/{addressId}", hasBody = true)
    Call<AddressListResponse> deleteAddress(
            @Path("addressId") String addressId,
            @Body Map<String, Object> body
    );

    @PUT("auth/addresses/{addressId}/default")
    Call<AddressListResponse> setDefaultAddress(
            @Path("addressId") String addressId,
            @Body Map<String, Object> body
    );
}
