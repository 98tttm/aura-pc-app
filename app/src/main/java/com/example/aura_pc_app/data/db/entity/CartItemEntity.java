package com.example.aura_pc_app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "cart_items", primaryKeys = {"productId", "variantId"})
public class CartItemEntity {

    @NonNull
    public String productId = "";

    @NonNull
    public String variantId = "";
    public String name;
    public String specs;
    public String imageUrl;
    public double unitPrice;
    public int quantity;
    public boolean synced;
    public long updatedAt;
}
