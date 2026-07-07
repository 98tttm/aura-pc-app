package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

public class DashboardTopProduct {
    @SerializedName("_id")
    public String name;

    public Integer totalQty;
    public Double totalRevenue;
}
