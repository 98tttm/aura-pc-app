package com.example.aura_pc_app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.aura_pc_app.data.db.entity.CartItemEntity;

import java.util.List;

@Dao
public interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCartItem(CartItemEntity cartItem);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCartItems(List<CartItemEntity> cartItems);

    @Query("SELECT * FROM cart_items ORDER BY updatedAt DESC")
    LiveData<List<CartItemEntity>> getAllCartItemsLive();

    @Query("SELECT * FROM cart_items ORDER BY updatedAt DESC")
    List<CartItemEntity> getCartItemsSync();

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items")
    LiveData<Integer> getCartItemCountLive();

    @Query("SELECT * FROM cart_items WHERE productId = :productId AND variantId = :variantId LIMIT 1")
    CartItemEntity getByCartKey(String productId, String variantId);

    @Query("UPDATE cart_items SET quantity = :quantity, synced = :synced, updatedAt = :updatedAt WHERE productId = :productId AND variantId = :variantId")
    void updateQuantity(String productId, String variantId, int quantity, boolean synced, long updatedAt);

    @Query("DELETE FROM cart_items WHERE productId = :productId AND variantId = :variantId")
    void deleteByCartKey(String productId, String variantId);

    @Query("DELETE FROM cart_items")
    void clearCart();

    @Transaction
    default void replaceCart(List<CartItemEntity> cartItems) {
        clearCart();
        if (cartItems != null && !cartItems.isEmpty()) {
            insertCartItems(cartItems);
        }
    }
}
