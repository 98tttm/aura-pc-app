package com.example.aura_pc_app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.aura_pc_app.data.db.entity.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProducts(List<ProductEntity> products);

    @Query("SELECT * FROM products")
    LiveData<List<ProductEntity>> getAllProductsLive();
}
