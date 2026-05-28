package com.example.aura_pc_app.data.api;

import com.example.aura_pc_app.data.db.entity.ProductEntity;
import java.util.List;

public class ProductResponse {
    public List<ProductEntity> items;
    public int total;
}