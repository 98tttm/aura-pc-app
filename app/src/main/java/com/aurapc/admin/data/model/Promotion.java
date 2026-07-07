package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Promotion implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    public String code;
    public String name;
    public String description;

    public String discountType; // percent | fixed
    public Double discountValue;
    public Double discountPercent;
    public Double maxDiscountAmount;
    public Double minOrderAmount;

    public Integer maxUsage;
    public Integer usedCount;
    public Integer maxUsagePerUser;

    public String startDate;
    public String endDate;
    public String expiresAt;

    public Boolean isActive;
    public Boolean active;

    public String createdAt;

    public boolean isExpired() {
        if (endDate != null) {
            try {
                java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(endDate);
                return d != null && d.getTime() < System.currentTimeMillis();
            } catch (Exception ex) {}
        }
        return false;
    }

    public boolean active() {
        if (isActive != null) return isActive;
        if (active != null) return active;
        return true;
    }
}