package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class OrderStatusSummary {
    @SerializedName("byStatus")
    public Map<String, StatusBucket> byStatus;

    @SerializedName("totalOrders")
    public int totalOrders;

    public static class StatusBucket {
        @SerializedName("count")
        public int count;

        @SerializedName("total")
        public double total;
    }

    public int countFor(String status) {
        if (byStatus == null) return 0;
        StatusBucket b = byStatus.get(status);
        return b == null ? 0 : b.count;
    }
}