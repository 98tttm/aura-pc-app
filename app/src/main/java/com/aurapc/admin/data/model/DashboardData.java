package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardData {
    // Backend `revenueThisMonth` is computed from delivered orders this month
    public Double totalRevenue;
    public Double revenueThisMonth;
    public Double revenueLastMonth;

    public Integer totalOrders;
    public Integer ordersThisMonth;
    public Integer ordersLastMonth;

    public Integer totalUsers;
    public Integer usersThisMonth;
    public Integer usersLastMonth;

    public Integer totalProducts;

    public Map<String, Integer> ordersByStatus;

    public List<Order> recentOrders;
    public List<DashboardTopProduct> topProducts;
    public List<Product> lowStockProducts;

    public Double getRevenueGrowthPercent() {
        if (revenueLastMonth == null || revenueLastMonth == 0) return 0d;
        if (revenueThisMonth == null) return 0d;
        return ((revenueThisMonth - revenueLastMonth) / revenueLastMonth) * 100d;
    }
}
