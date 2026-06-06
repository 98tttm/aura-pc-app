package com.aura.pc.ui.cart;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.data.repository.AppRepository;
import com.example.aura_pc_app.ui.base.BaseViewModel;
import java.util.List;

public class CartViewModel extends BaseViewModel {
    private final AppRepository repository;
    private LiveData<List<CartItemEntity>> cartItems;

    public CartViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(
                application,
                ApiClient.getInstance(application).getApiService()
        );
    }

    public LiveData<List<CartItemEntity>> getCartItems() {
        if (cartItems == null) {
            cartItems = repository.getCartItems();
        }
        return cartItems;
    }
}
