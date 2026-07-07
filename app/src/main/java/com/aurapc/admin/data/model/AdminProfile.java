package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Admin profile returned by /api/admin/auth/me and login result.
 */
public class AdminProfile implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("email")
    public String email;

    @SerializedName("name")
    public String name;

    @SerializedName("role")
    public String role;

    @SerializedName("avatar")
    public String avatar;

    @SerializedName("permissions")
    public List<String> permissions;

    @SerializedName("isActive")
    public Boolean isActive;

    @SerializedName("lastLogin")
    public String lastLogin;

    @SerializedName("createdAt")
    public String createdAt;

    public boolean isSuperAdmin() {
        return "super_admin".equalsIgnoreCase(role);
    }
}