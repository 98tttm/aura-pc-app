package com.example.aura_pc_app.data.api;

import com.aura.pc.ui.address.AddressListResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.Part;
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

    @GET("categories")
    Call<java.util.List<Map<String, Object>>> getCategories();

    @GET("products")
    Call<Map<String, Object>> getProducts();

    @GET("products/{id}")
    Call<Map<String, Object>> getProductById(@Path("id") String id);

    @GET("reviews")
    Call<Map<String, Object>> getProductReviews(@Query("productId") String productId);

    @POST("coupons/validate")
    Call<Map<String, Object>> validateCoupon(@Body Map<String, Object> body);

    @POST("promotions/validate")
    Call<Map<String, Object>> validatePromotion(@Body Map<String, Object> body);

    @POST("payment/momo/create")
    Call<Map<String, Object>> createMoMoPayment(@Body Map<String, Object> body);

    @POST("payment/zalopay/create")
    Call<Map<String, Object>> createZaloPayPayment(@Body Map<String, Object> body);

    @POST("payment/vnpay/create")
    Call<Map<String, Object>> createVnPayPayment(@Body Map<String, Object> body);

    @POST("payment/vietqr/create")
    Call<Map<String, Object>> createVietQrPayment(@Body Map<String, Object> body);

    @GET("products")
    Call<Map<String, Object>> getProductsByCategory(
            @Query("category") String categoryId,
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("products")
    Call<Map<String, Object>> getProductsFiltered(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("category") String category,
            @Query("brand") String brand,
            @Query("minPrice") Double minPrice,
            @Query("maxPrice") Double maxPrice,
            @Query("minRating") Double minRating,
            @Query("inStock") Boolean inStock,
            @Query("sort") String sort,
            @Query("search") String search
    );

    @POST("auth/follow/{targetUserId}")
    Call<Map<String, Object>> toggleFollow(@Path("targetUserId") String targetUserId);

    @GET("auth/following/{userId}")
    Call<Map<String, Object>> getFollowing(@Path("userId") String userId);

    @GET("hub/posts")
    Call<Map<String, Object>> getHubPosts(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("topic") String topic,
            @Query("sort") String sort
    );

    @GET("hub/posts/{id}")
    Call<Map<String, Object>> getHubPost(@Path("id") String id);

    @POST("hub/posts")
    Call<Map<String, Object>> createHubPost(@Body Map<String, Object> body);

    @Multipart
    @POST("hub/upload")
    Call<Map<String, Object>> uploadHubImages(@Part List<MultipartBody.Part> parts);

    @POST("hub/posts/{postId}/like")
    Call<Map<String, Object>> toggleHubLike(@Path("postId") String postId);

    @GET("hub/posts/{postId}/comments")
    Call<java.util.List<Map<String, Object>>> getHubComments(
            @Path("postId") String postId,
            @Query("sort") String sort
    );

    @POST("hub/posts/{postId}/comments")
    Call<Map<String, Object>> createHubComment(
            @Path("postId") String postId,
            @Body Map<String, Object> body
    );

    @GET("hub/topics")
    Call<java.util.List<String>> getHubTopics();

    @GET("hub/trending")
    Call<java.util.List<Map<String, Object>>> getHubTrending(@Query("limit") int limit);

    @GET("blogs")
    Call<Map<String, Object>> getBlogs(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("builders/{id}")
    Call<Map<String, Object>> getBuilder(@Path("id") String id);

    @POST("builders")
    Call<Map<String, Object>> createBuilder(@Body Map<String, Object> body);

    @PUT("builders/{id}")
    Call<Map<String, Object>> updateBuilderComponent(
            @Path("id") String id,
            @Body Map<String, Object> body
    );

    @GET("users/{id}")
    Call<Map<String, Object>> getUserById(@Path("id") int id);

    @GET("users/{id}")
    Call<Map<String, Object>> getUserById(@Path("id") String id);

    @GET("users/me")
    Call<Map<String, Object>> getCurrentUser();

    @GET("users/me")
    Call<Map<String, Object>> getMyProfile();

    @PATCH("users/me")
    Call<Map<String, Object>> updateMyProfile(@Body Map<String, Object> profileData);

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

    @POST("orders")
    Call<Map<String, Object>> createOrder(@Body Map<String, Object> request);

    @GET("cart")
    Call<CartResponse> getCart(@Query("userId") String userId);

    @POST("cart/add")
    Call<CartResponse> addCartItem(@Body Map<String, Object> body);

    @PUT("cart/update")
    Call<CartResponse> updateCartItem(@Body Map<String, Object> body);

    @DELETE("cart/remove")
    Call<CartResponse> removeCartItem(
            @Query("userId") String userId,
            @Query("productId") String productId
    );

    @GET("orders")
    Call<Object> getOrders();

    @GET("orders/my")
    Call<Object> getMyOrders();

    @GET("orders/me")
    Call<Object> getCurrentUserOrders();

    @GET("notifications")
    Call<Map<String, Object>> getNotifications(
            @Query("limit") int limit,
            @Query("unreadOnly") boolean unreadOnly
    );

    @PATCH("notifications/{id}/read")
    Call<Map<String, Object>> markNotificationRead(
            @Path("id") String id,
            @Body Map<String, Object> body
    );

    @PATCH("notifications/read-all")
    Call<Map<String, Object>> markAllNotificationsRead(@Body Map<String, Object> body);

    @PATCH("orders/{id}")
    Call<Object> updateOrder(
            @Path("id") String orderId,
            @Body Map<String, Object> body
    );

    @GET("orders/{id}")
    Call<Map<String, Object>> getOrderById(@Path("id") String orderId);

    @POST("orders/{orderNumber}/confirm-received")
    Call<Object> confirmOrderReceived(
            @Path("orderNumber") String orderNumber,
            @Body Map<String, Object> body
    );

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
