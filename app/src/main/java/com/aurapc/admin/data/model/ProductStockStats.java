package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProductStockStats {
    @SerializedName("stats")
    public List<StockBucket> stats;

    @SerializedName("total")
    public int total;

    public static class StockBucket {
        @SerializedName("name")
        public String name;
        @SerializedName("count")
        public int count;
    }
}