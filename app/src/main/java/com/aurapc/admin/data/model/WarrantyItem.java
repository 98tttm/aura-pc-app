package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class WarrantyItem implements Serializable {

    @SerializedName("serialNumber")
    public String serialNumber;

    @SerializedName("productName")
    public String productName;

    @SerializedName("productSlug")
    public String productSlug;

    @SerializedName("productImage")
    public String productImage;

    @SerializedName("qty")
    public int qty;

    @SerializedName("price")
    public double price;

    @SerializedName("warrantyMonths")
    public int warrantyMonths;

    @SerializedName("orderNumber")
    public String orderNumber;

    @SerializedName("customerName")
    public String customerName;

    @SerializedName("customerPhone")
    public String customerPhone;

    @SerializedName("purchaseDate")
    public String purchaseDate;

    @SerializedName("expiryDate")
    public String expiryDate;

    @SerializedName("status")
    public String status;

    public boolean isValid() {
        return "valid".equalsIgnoreCase(status);
    }
}