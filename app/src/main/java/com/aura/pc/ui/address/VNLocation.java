package com.aura.pc.ui.address;

import java.util.List;

/**
 * Đơn vị hành chính VN trả về từ https://provinces.open-api.vn
 * Dùng chung cho Tỉnh/Thành (có districts), Quận/Huyện (có wards) và Phường/Xã.
 */
public class VNLocation {
    public String name;
    public int code;
    public String division_type;
    public String codename;
    public List<VNLocation> districts;
    public List<VNLocation> wards;

    @Override
    public String toString() {
        // Spinner adapter mặc định hiển thị bằng toString()
        return name != null ? name : "";
    }
}
