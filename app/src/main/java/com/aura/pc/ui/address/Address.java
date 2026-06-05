package com.aura.pc.ui.address;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Mô hình một địa chỉ trong sổ địa chỉ — khớp 1:1 với address sub-schema của backend
 * (server/models/User.js) và interface Address bên website (address.service.ts).
 */
public class Address implements Serializable {

    @SerializedName("_id")
    public String id;

    public String label = "Nhà riêng";   // "Nhà riêng" | "Công ty" | "Khác"
    public String fullName = "";
    public String phone = "";
    public String city = "";              // Tỉnh / Thành phố
    public String district = "";          // Quận / Huyện
    public String ward = "";              // Phường / Xã
    public String address = "";           // Số nhà, tên đường
    public boolean isDefault = false;

    public Address() {}

    /** Ghép địa chỉ đầy đủ để hiển thị: "số nhà, phường, quận, thành phố". */
    public String formattedAddress() {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, address);
        appendPart(sb, ward);
        appendPart(sb, district);
        appendPart(sb, city);
        return sb.toString();
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part == null) return;
        String trimmed = part.trim();
        if (trimmed.isEmpty()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(trimmed);
    }
}
