package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

public class RevenueChartPoint {
    @SerializedName("_id")
    public String _id;

    public String label;

    public Double revenue;
    public Integer orders;
    public Integer newCustomers;
}
