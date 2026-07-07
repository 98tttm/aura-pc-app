package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class CustomerUser {
    @SerializedName("_id")
    public String id;
    public String phoneNumber;
    public String username;
    public String email;
    public String avatar;
    public String name;
    public Boolean isActive;
    public Boolean active;
    public Integer orderCount;
    public Double totalSpent;
    public List<Order> recentOrders;
    public Map<String, Object> profile;
    public String createdAt;
    public String updatedAt;
    public String lastLogin;

    public String getDisplayName() {
        if (name != null && !name.isEmpty()) return name;
        if (profile != null) {
            Object fn = profile.get("fullName");
            if (fn != null) return fn.toString();
        }
        if (username != null && !username.isEmpty()) return username;
        if (phoneNumber != null && !phoneNumber.isEmpty()) return phoneNumber;
        return "Khách hàng";
    }

    public String getPhone() {
        return phoneNumber != null ? phoneNumber : phone;
    }

    private String phone;

    public boolean active() {
        if (isActive != null) return isActive;
        if (active != null) return active;
        return true;
    }
}
