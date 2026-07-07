package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ProductReview implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("product")
    public Object product;

    @SerializedName("user")
    public Object user;

    @SerializedName("type")
    public String type;

    @SerializedName("rating")
    public Integer rating;

    @SerializedName("content")
    public String content;

    @SerializedName("images")
    public List<String> images;

    @SerializedName("flagged")
    public Boolean flagged;

    @SerializedName("hidden")
    public Boolean hidden;

    @SerializedName("flagReason")
    public String flagReason;

    public String productName;
    public String userName;
    public String author;

    @SerializedName("createdAt")
    public String createdAt;

    public String productName() {
        if (product instanceof java.util.Map) {
            Object name = ((java.util.Map<?, ?>) product).get("name");
            if (name != null) return String.valueOf(name);
        }
        return "Sản phẩm";
    }

    public String userName() {
        if (user instanceof java.util.Map) {
            Object profile = ((java.util.Map<?, ?>) user).get("profile");
            if (profile instanceof java.util.Map) {
                Object fullName = ((java.util.Map<?, ?>) profile).get("fullName");
                if (fullName != null) return String.valueOf(fullName);
            }
            Object phone = ((java.util.Map<?, ?>) user).get("phoneNumber");
            if (phone != null) return String.valueOf(phone);
        }
        return "Người dùng";
    }
}