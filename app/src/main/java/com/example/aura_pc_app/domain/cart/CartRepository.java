package com.example.aura_pc_app.domain.cart;

import androidx.lifecycle.LiveData;

import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.data.db.entity.ProductEntity;

import java.util.List;

public interface CartRepository {
    interface CartCallback {
        void onSuccess();
        void onError(String message);
    }

    LiveData<List<CartItemEntity>> observeCartItems();
    LiveData<Integer> observeCartItemCount();
    void addProduct(ProductEntity product, int quantity, CartCallback callback);
    void updateQuantity(CartItemEntity item, int quantity, CartCallback callback);
    void removeItem(CartItemEntity item, CartCallback callback);
    void syncCartAfterLogin(CartCallback callback);
}
