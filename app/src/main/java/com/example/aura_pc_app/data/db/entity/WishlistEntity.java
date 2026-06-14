package com.example.aura_pc_app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "wishlist_items")
public class WishlistEntity {

    @PrimaryKey
    @NonNull
    public String productId = "";

    public String name;
    public double price;
    public double oldPrice;
    public String imageUrl;
    public long addedAt;

    public WishlistEntity() {}

    @Ignore
    public WishlistEntity(@NonNull String productId, String name, double price, double oldPrice, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.oldPrice = oldPrice;
        this.imageUrl = imageUrl;
        this.addedAt = System.currentTimeMillis();
    }
}
