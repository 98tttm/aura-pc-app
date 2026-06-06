package com.example.aura_pc_app.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.repository.AppRepository;
import com.example.aura_pc_app.ui.base.BaseViewModel;
import java.util.List;

public class HomeViewModel extends BaseViewModel {
    private final AppRepository repository;
    private LiveData<List<ProductEntity>> products;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(
                application,
                ApiClient.getInstance(application).getApiService()
        );
    }

    public LiveData<List<ProductEntity>> getProducts() {
        if (products == null) {
            products = repository.getProducts();
        }
        return products;
    }
}
