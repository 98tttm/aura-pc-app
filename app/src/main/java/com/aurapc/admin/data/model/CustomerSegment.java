package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class CustomerSegment {
    @SerializedName("summary")
    public List<SegmentBucket> summary;

    @SerializedName("totalUsers")
    public int totalUsers;

    @SerializedName("totalCustomers")
    public int totalCustomers;

    @SerializedName("newCustomersLast30Days")
    public int newCustomersLast30Days;

    @SerializedName("segments")
    public Map<String, List<SegmentEntry>> segments;

    public static class SegmentBucket {
        @SerializedName("name")
        public String name;
        @SerializedName("count")
        public int count;
        @SerializedName("totalRevenue")
        public double totalRevenue;
    }

    public static class SegmentEntry {
        @SerializedName("userId")
        public String userId;
        @SerializedName("totalSpent")
        public double totalSpent;
        @SerializedName("orderCount")
        public int orderCount;
    }
}