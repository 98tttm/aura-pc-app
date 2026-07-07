package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class OrderItem {
    @SerializedName("_id")
    public String id;
    public String product;
    public String name;
    public String image;
    public Integer qty;
    public Integer quantity;
    public Double price;
    public String serialNumber;
}
