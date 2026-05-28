package com.example.aura_pc_app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cart_items")
public class CartItemEntity {

    @PrimaryKey
    @NonNull
    public String productId = ""; // Cấp giá trị rỗng mặc định để tránh lỗi Null

    public int quantity;
}