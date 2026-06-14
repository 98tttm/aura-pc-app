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
import com.google.gson.Gson;
import com.google.gson.JsonElement;

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
    private final Gson gson = new Gson();

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

    public LiveData<List<CartItemEntity>> getCartItems() {
        return cartDao.getAllCartItemsLive();
    }

    public void addToCart(String productId, int quantity) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }
        executor.execute(() -> {
            CartItemEntity item = new CartItemEntity();
            item.productId = productId;
            item.quantity = Math.max(1, quantity);
            cartDao.insertCartItem(item);
        });
    }

    private void refreshProductsFromNetwork() {
        // Sử dụng tên hàm mới getProductsPaginated đã thống nhất với ApiService
        apiService.getProductsPaginated(1, 50).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductEntity> remoteItems = mapProducts(response.body().items);
                    if (!remoteItems.isEmpty()) {
                        executor.execute(() -> {
                            productDao.deleteGeneratedProductFixtures();
                            productDao.insertProducts(remoteItems);
                        });
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

    private List<ProductEntity> mapProducts(List<ProductResponse.Item> items) {
        List<ProductEntity> products = new ArrayList<>();
        if (items == null) return products;
        for (ProductResponse.Item item : items) {
            if (item == null || item._id == null || item._id.trim().isEmpty()) {
                continue;
            }
            ProductEntity product = new ProductEntity();
            product._id = item._id;
            product.name = item.name;
            product.slug = item.slug;
            product.price = item.price;
            product.salePrice = item.salePrice;
            product.category_id = item.category_id;
            product.category_ids = item.category_ids;
            product.images = firstImageUrl(item.images);
            product.specs = specsText(item.specs);
            product.brand = item.brand;
            product.stock = item.stock;
            product.active = item.active == null || item.active;
            products.add(product);
        }
        return products;
    }

    private String firstImageUrl(JsonElement images) {
        if (images == null || images.isJsonNull()) return null;
        if (images.isJsonArray() && images.getAsJsonArray().size() > 0) {
            JsonElement first = images.getAsJsonArray().get(0);
            return first == null || first.isJsonNull() ? null : first.getAsString();
        }
        if (images.isJsonPrimitive()) {
            return images.getAsString();
        }
        return gson.toJson(images);
    }

    private String specsText(JsonElement specs) {
        if (specs == null || specs.isJsonNull()) return null;
        if (!specs.isJsonObject()) return specs.toString();

        List<String> summary = new ArrayList<>();
        addSpec(summary, specs, "CPU");
        addSpec(summary, specs, "RAM");
        addSpec(summary, specs, "Card đồ họa");
        addSpec(summary, specs, "VGA");
        addSpec(summary, specs, "Ổ cứng");
        addSpec(summary, specs, "Ổ cứng");
        addSpec(summary, specs, "Màn hình");
        if (summary.isEmpty()) {
            return gson.toJson(specs);
        }
        return String.join(", ", summary);
    }

    private void addSpec(List<String> summary, JsonElement specs, String key) {
        JsonElement value = specs.getAsJsonObject().get(key);
        if (value != null && !value.isJsonNull()) {
            String text = value.getAsString();
            if (text != null && !text.trim().isEmpty()) {
                summary.add(text.trim());
            }
        }
    }
}
