package com.example.aura_pc_app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.aura_pc_app.data.db.entity.WishlistEntity;

import java.util.List;

@Dao
public interface WishlistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WishlistEntity item);

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    void deleteByProductId(String productId);

    @Query("SELECT * FROM wishlist_items ORDER BY addedAt DESC")
    LiveData<List<WishlistEntity>> getAllLive();

    @Query("SELECT * FROM wishlist_items ORDER BY addedAt DESC")
    List<WishlistEntity> getAllSync();

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE productId = :productId)")
    boolean isFavorite(String productId);

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE productId = :productId)")
    LiveData<Boolean> isFavoriteLive(String productId);

    @Query("DELETE FROM wishlist_items")
    void clearAll();
}
