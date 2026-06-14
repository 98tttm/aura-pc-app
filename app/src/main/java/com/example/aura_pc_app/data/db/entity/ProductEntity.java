package com.example.aura_pc_app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.List;

@Entity(tableName = "products")
public class ProductEntity {

    @PrimaryKey
    @NonNull
    public String _id = ""; // Cấp giá trị rỗng mặc định để tránh lỗi Null

    public String name;
    public String slug;
    public double price;
    public Double salePrice;
    public String category_id;
    public List<String> category_ids;

    // Các trường Mixed của MongoDB sẽ lưu dưới dạng JSON String
    public String images;
    public String specs;

    public String brand;
    public int stock;
    public boolean active;

    @Ignore
    public Double oldPrice;

    @Ignore
    public String imageUrl;
}
