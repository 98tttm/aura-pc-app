package com.aura.pc.ui.cart;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.aura_pc_app.data.cart.CartRepositoryImpl;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.ui.base.BaseViewModel;

import java.util.List;

public class CartViewModel extends BaseViewModel {
    private final CartRepositoryImpl repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private LiveData<List<CartItemEntity>> cartItems;

    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new CartRepositoryImpl(application);
    }

    public LiveData<List<CartItemEntity>> getCartItems() {
        if (cartItems == null) {
            cartItems = repository.observeCartItems();
        }
        return cartItems;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void increment(CartItemEntity item) {
        updateQuantity(item, Math.max(1, item.quantity) + 1);
    }

    public void decrement(CartItemEntity item) {
        updateQuantity(item, Math.max(1, item.quantity) - 1);
    }

    public void remove(CartItemEntity item) {
        loading.setValue(true);
        repository.removeItem(item, callback());
    }

    public void refreshMissingPrices(List<CartItemEntity> items) {
        repository.refreshMissingPrices(items);
    }

    private void updateQuantity(CartItemEntity item, int quantity) {
        loading.setValue(true);
        repository.updateQuantity(item, quantity, callback());
    }

    private com.example.aura_pc_app.domain.cart.CartRepository.CartCallback callback() {
        return new com.example.aura_pc_app.domain.cart.CartRepository.CartCallback() {
            @Override
            public void onSuccess() {
                loading.setValue(false);
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                postError(message);
            }
        };
    }
}
