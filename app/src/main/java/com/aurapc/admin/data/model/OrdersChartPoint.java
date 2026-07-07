package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

public class OrdersChartPoint {
    @SerializedName("_id")
    public String _id;

    public Integer count;
    public Double revenue;
}
