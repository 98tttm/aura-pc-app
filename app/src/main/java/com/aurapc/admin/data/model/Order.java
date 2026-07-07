package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class Order {
    public String id;
    @SerializedName("_id")
    public String _id;
    public String orderNumber;
    public String status;
    public String createdAt;
    public String updatedAt;
    public ShippingAddress shippingAddress;
    public List<OrderItem> items;
    public Double subtotal;
    public Double shippingFee;
    public Double discountAmount;
    public Double total;
    public Double totalAmount;
    public String paymentMethod;
    public String paymentStatus;
    public String cancelRequestStatus;
    public String returnRequestStatus;

    public Map<String, Object> user;

    public static class ShippingAddress {
        public String name;
        public String fullName;
        public String phone;
        public String street;
        public String address;
        public String city;
        public String district;
        public String ward;
    }

    public static class OrderItem {
        public String id;
        @SerializedName("_id")
        public String _id;
        public String product;
        public Map<String, Object> productData;
        public String name;
        public String image;
        public Integer qty;
        public Integer quantity;
        public Double price;
        public String serialNumber;
    }
}
