package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SupportMessage implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("conversation")
    public String conversation;

    @SerializedName("senderType")
    public String senderType; // 'admin' | 'user'

    @SerializedName("senderAdmin")
    public Object senderAdmin;

    @SerializedName("senderUser")
    public Object senderUser;

    @SerializedName("content")
    public String content;

    @SerializedName("createdAt")
    public String createdAt;

    public boolean isFromAdmin() {
        return "admin".equalsIgnoreCase(senderType);
    }

    public String senderName() {
        if (isFromAdmin()) {
            if (senderAdmin instanceof java.util.Map) {
                Object name = ((java.util.Map<?, ?>) senderAdmin).get("name");
                if (name != null) return String.valueOf(name);
                Object email = ((java.util.Map<?, ?>) senderAdmin).get("email");
                if (email != null) return String.valueOf(email);
            }
            return "Admin";
        }
        if (senderUser instanceof java.util.Map) {
            java.util.Map<?, ?> m = (java.util.Map<?, ?>) senderUser;
            Object phone = m.get("phoneNumber");
            if (phone != null) return String.valueOf(phone);
            Object profile = m.get("profile");
            if (profile instanceof java.util.Map) {
                Object fn = ((java.util.Map<?, ?>) profile).get("fullName");
                if (fn != null) return String.valueOf(fn);
            }
            Object un = m.get("username");
            if (un != null) return String.valueOf(un);
        }
        return "Khách hàng";
    }
}