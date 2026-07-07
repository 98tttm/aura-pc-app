package com.aurapc.admin.data.api;

import com.aurapc.admin.data.model.Order;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OrderApi {

    @GET("api/admin/orders")
    Call<OrderListResponse> getOrders(
            @Query("status") String status,
            @Query("search") String search,
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("api/admin/orders/{orderNumber}")
    Call<Order> getOrderDetail(@Path("orderNumber") String orderNumber);

    @PUT("api/admin/orders/{orderNumber}/status")
    Call<Order> updateStatus(@Path("orderNumber") String orderNumber, @Body Map<String, String> body);

    @PUT("api/admin/orders/{orderNumber}/cancel")
    Call<Order> cancel(@Path("orderNumber") String orderNumber, @Body Map<String, Object> body);

    @POST("api/admin/extra-orders/bulk-update")
    Call<Map<String, Object>> bulkUpdateStatus(@Body BulkOrderUpdate body);

    class OrderListResponse {
        public List<Order> items;
        public int total;
        public int page;
        public int limit;
    }

    class BulkOrderUpdate {
        public List<String> orderNumbers;
        public String status;
    }
}