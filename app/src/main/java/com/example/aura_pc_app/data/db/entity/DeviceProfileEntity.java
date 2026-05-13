package com.example.aura_pc_app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_profile")
public class DeviceProfileEntity {

    @PrimaryKey
    @NonNull
    public String deviceId = ""; // Cấp giá trị rỗng mặc định để tránh lỗi Null

    public boolean isDarkMode;
}