package com.aurapc.admin.data.api;

import com.aurapc.admin.data.model.DashboardData;
import com.aurapc.admin.data.model.DashboardTopProduct;
import com.aurapc.admin.data.model.OrderStatusSummary;
import com.aurapc.admin.data.model.RevenueChartPoint;
import com.aurapc.admin.data.model.OrdersChartPoint;
import com.aurapc.admin.data.model.CustomerSegment;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DashboardApi {

    @GET("api/admin/dashboard/stats")
    Call<DashboardData> getStats();

    @GET("api/admin/dashboard/chart/orders")
    Call<List<OrdersChartPoint>> getOrdersChart(@Query("days") int days);

    @GET("api/admin/dashboard/chart/revenue")
    Call<List<RevenueChartPoint>> getRevenueChart(@Query("months") int months);

    @GET("api/admin/dashboard/chart/revenue-weekly")
    Call<List<RevenueChartPoint>> getRevenueWeekly();

    @GET("api/admin/dashboard/top-products")
    Call<List<DashboardTopProduct>> getTopProducts(@Query("limit") int limit);

    @GET("api/admin/extra-orders/status-summary")
    Call<OrderStatusSummary> getOrderStatusSummary();

    @GET("api/admin/extra-users/segment")
    Call<CustomerSegment> getCustomerSegment();
}