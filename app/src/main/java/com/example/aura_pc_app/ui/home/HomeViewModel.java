package com.example.aura_pc_app.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.cart.CartRepositoryImpl;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.repository.AppRepository;
import com.example.aura_pc_app.domain.cart.CartRepository;
import com.example.aura_pc_app.ui.base.BaseViewModel;

import java.util.List;

public class HomeViewModel extends BaseViewModel {
    private final AppRepository repository;
    private final CartRepository cartRepository;
    private final MutableLiveData<Boolean> productAdded = new MutableLiveData<>(false);
    private LiveData<List<ProductEntity>> products;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(
                application,
                ApiClient.getInstance(application).getApiService()
        );
        cartRepository = new CartRepositoryImpl(application);
    }

    public LiveData<List<ProductEntity>> getProducts() {
        if (products == null) {
            products = repository.getProducts();
        }
        return products;
    }

    public LiveData<Boolean> getProductAdded() {
        return productAdded;
    }

    public void addProductToCart(ProductEntity product) {
        cartRepository.addProduct(product, 1, new CartRepository.CartCallback() {
            @Override
            public void onSuccess() {
                productAdded.setValue(true);
            }

            @Override
            public void onError(String message) {
                postError(message);
            }
        });
    }

    public void clearProductAdded() {
        productAdded.setValue(false);
    }
}
