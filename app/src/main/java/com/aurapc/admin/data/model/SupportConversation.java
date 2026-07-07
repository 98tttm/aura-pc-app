package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class SupportConversation implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("user")
    public Object user;

    @SerializedName("assignedAdmin")
    public Object assignedAdmin;

    @SerializedName("archived")
    public Boolean archived;

    @SerializedName("lastMessagePreview")
    public String lastMessagePreview;

    @SerializedName("lastMessageAt")
    public String lastMessageAt;

    @SerializedName("lastMessageBy")
    public String lastMessageBy;

    @SerializedName("unreadForAdmin")
    public Integer unreadForAdmin;

    @SerializedName("unreadForUser")
    public Integer unreadForUser;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedAt")
    public String updatedAt;

    public String userName() {
        if (user instanceof java.util.Map) {
            Object phone = ((java.util.Map<?, ?>) user).get("phoneNumber");
            if (phone != null) return String.valueOf(phone);
            Object email = ((java.util.Map<?, ?>) user).get("email");
            if (email != null) return String.valueOf(email);
            Object profile = ((java.util.Map<?, ?>) user).get("profile");
            if (profile instanceof java.util.Map) {
                Object fullName = ((java.util.Map<?, ?>) profile).get("fullName");
                if (fullName != null) return String.valueOf(fullName);
            }
        }
        return "Khách hàng";
    }

    public String adminName() {
        if (assignedAdmin instanceof java.util.Map) {
            Object name = ((java.util.Map<?, ?>) assignedAdmin).get("name");
            if (name != null) return String.valueOf(name);
            Object email = ((java.util.Map<?, ?>) assignedAdmin).get("email");
            if (email != null) return String.valueOf(email);
        }
        return null;
    }
}