package com.example.aura_pc_app.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.data.api.CartResponse;
import com.example.aura_pc_app.data.api.ProductResponse;
import com.example.aura_pc_app.data.api.SyncCartRequest;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.dao.CartDao;
import com.example.aura_pc_app.data.db.dao.ProductDao;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.data.db.entity.ProductEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppRepository {
    private final ProductDao productDao;
    private final CartDao cartDao;
    private final ApiService apiService;
    private final ExecutorService executor;

    public AppRepository(Application application, ApiService apiService) {
        AppDatabase db = AppDatabase.getInstance(application);
        this.productDao = db.productDao();
        this.cartDao = db.cartDao();
        this.apiService = apiService;

        // Tạo luồng xử lý ngầm để không gây lag giao diện
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Lấy danh sách sản phẩm:
     * Ưu tiên hiển thị từ Local DB (Room), đồng thời cập nhật mới từ Network.
     */
    public LiveData<List<ProductEntity>> getProducts() {
        refreshProductsFromNetwork();
        return productDao.getAllProductsLive();
    }

    private void refreshProductsFromNetwork() {
        // Sử dụng tên hàm mới getProductsPaginated đã thống nhất với ApiService
        apiService.getProductsPaginated(1, 50).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductEntity> remoteItems = response.body().items;
                    if (remoteItems != null) {
                        executor.execute(() -> productDao.insertProducts(remoteItems));
                    }
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                // Khi rớt mạng, LiveData vẫn giữ dữ liệu cũ trong Room để hiển thị
            }
        });
    }

    /**
     * Đồng bộ giỏ hàng offline lên Server.
     */
    public void syncOfflineCartToServer() {
        executor.execute(() -> {
            List<CartItemEntity> offlineItems = cartDao.getCartItemsSync();
            if (offlineItems == null || offlineItems.isEmpty()) return;

            List<SyncCartRequest.Item> syncItems = new ArrayList<>();
            for (CartItemEntity entity : offlineItems) {
                syncItems.add(new SyncCartRequest.Item(entity.productId, entity.quantity));
            }

            apiService.syncCartToServer(new SyncCartRequest(syncItems)).enqueue(new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().success) {
                        // Đồng bộ thành công thì dọn dẹp (Local DB)
                        executor.execute(cartDao::clearCart);
                    }
                }

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {
                    // Thất bại thì vẫn giữ trong DB để đợi lần sau có mạng sẽ sync lại
                }
            });
        });
    }
}