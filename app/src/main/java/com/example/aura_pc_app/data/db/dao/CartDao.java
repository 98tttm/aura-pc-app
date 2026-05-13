package com.example.aura_pc_app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.aura_pc_app.data.db.entity.CartItemEntity;

import java.util.List;

@Dao
public interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCartItem(CartItemEntity cartItem);

    @Query("SELECT * FROM cart_items")
    LiveData<List<CartItemEntity>> getAllCartItemsLive();

    @Query("SELECT * FROM cart_items")
    List<CartItemEntity> getCartItemsSync();

    @Query("DELETE FROM cart_items")
    void clearCart();
}