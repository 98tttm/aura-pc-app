package com.example.aura_pc_app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "price_history")
public class PriceHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String productId;
    public double oldPrice;

    // Room DB không lưu được kiểu Date trực tiếp dễ dàng, nên ta dùng Long (Timestamp)
    public Long updateTimestamp;
}