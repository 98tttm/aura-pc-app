package com.example.aura_pc_app.data.api;

import com.aura.pc.ui.address.AddressListResponse;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query; // Thêm thư viện Query cho phân trang

public interface ApiService {
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @GET("products")
    Call<Map<String, Object>> getProducts();

    @GET("products")
    Call<Map<String, Object>> getProductsPaginatedMap(
            @Query("page") int page,
            @Query("limit") int limit
    );

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

    // ===================== SỔ ĐỊA CHỈ (Address Book) =====================

    /** Lấy toàn bộ địa chỉ của user. */
    @GET("auth/addresses/{userId}")
    Call<AddressListResponse> getAddresses(@Path("userId") String userId);

    /** Thêm địa chỉ mới. Body chứa: userId, label, fullName, phone, city, district, ward, address, isDefault. */
    @POST("auth/addresses")
    Call<AddressListResponse> addAddress(@Body Map<String, Object> body);

    /** Cập nhật một địa chỉ theo addressId. */
    @PUT("auth/addresses/{addressId}")
    Call<AddressListResponse> updateAddress(@Path("addressId") String addressId,
                                            @Body Map<String, Object> body);

    /** Xóa một địa chỉ. DELETE có body (userId) nên dùng @HTTP. */
    @HTTP(method = "DELETE", path = "auth/addresses/{addressId}", hasBody = true)
    Call<AddressListResponse> deleteAddress(@Path("addressId") String addressId,
                                            @Body Map<String, Object> body);

    /** Đặt một địa chỉ làm mặc định. */
    @PUT("auth/addresses/{addressId}/default")
    Call<AddressListResponse> setDefaultAddress(@Path("addressId") String addressId,
                                                @Body Map<String, Object> body);
}
