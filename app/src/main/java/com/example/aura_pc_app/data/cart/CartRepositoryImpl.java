package com.example.aura_pc_app.data.cart;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.data.api.CartResponse;
import com.example.aura_pc_app.data.api.SyncCartRequest;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.dao.CartDao;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.domain.cart.CartItem;
import com.example.aura_pc_app.domain.cart.CartMergeUseCase;
import com.example.aura_pc_app.domain.cart.CartRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class CartRepositoryImpl implements CartRepository {
    private final CartDao cartDao;
    private final ApiService apiService;
    private final TokenManager tokenManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CartMergeUseCase mergeUseCase = new CartMergeUseCase();

    public CartRepositoryImpl(Context context) {
        Context appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(appContext);
        ApiClient apiClient = ApiClient.getInstance(appContext);
        cartDao = db.cartDao();
        apiService = apiClient.getApiService();
        tokenManager = apiClient.getTokenManager();
    }

    @Override
    public LiveData<List<CartItemEntity>> observeCartItems() {
        return cartDao.getAllCartItemsLive();
    }

    @Override
    public LiveData<Integer> observeCartItemCount() {
        return cartDao.getCartItemCountLive();
    }

    @Override
    public void addProduct(ProductEntity product, int quantity, CartCallback callback) {
        if (product == null || product._id == null || product._id.trim().isEmpty()) {
            postError(callback, "Không thể thêm sản phẩm này vào giỏ hàng");
            return;
        }
        executor.execute(() -> {
            List<CartItemEntity> snapshot = cartDao.getCartItemsSync();
            CartItemEntity next = toCartEntity(product, Math.max(1, quantity));
            CartItemEntity existing = cartDao.getByCartKey(next.productId, next.variantId);
            if (existing != null) {
                next.quantity += Math.max(0, existing.quantity);
            }
            next.synced = !isLoggedIn();
            cartDao.insertCartItem(next);

            if (!isLoggedIn()) {
                postSuccess(callback);
                return;
            }

            try {
                Response<CartResponse> response = syncCurrentCart();
                handleRemoteMutation(response, snapshot, callback);
            } catch (IOException e) {
                rollback(snapshot, callback, "Không thể đồng bộ giỏ hàng. Vui lòng thử lại.");
            }
        });
    }

    @Override
    public void updateQuantity(CartItemEntity item, int quantity, CartCallback callback) {
        if (item == null || item.productId == null) return;
        if (quantity <= 0) {
            removeItem(item, callback);
            return;
        }
        executor.execute(() -> {
            List<CartItemEntity> snapshot = cartDao.getCartItemsSync();
            cartDao.updateQuantity(item.productId, normalizedVariant(item.variantId), quantity, !isLoggedIn(), System.currentTimeMillis());

            if (!isLoggedIn()) {
                postSuccess(callback);
                return;
            }

            try {
                Response<CartResponse> response = syncCurrentCart();
                handleRemoteMutation(response, snapshot, callback);
            } catch (IOException e) {
                rollback(snapshot, callback, "Không thể cập nhật số lượng. Vui lòng thử lại.");
            }
        });
    }

    @Override
    public void removeItem(CartItemEntity item, CartCallback callback) {
        if (item == null || item.productId == null) return;
        executor.execute(() -> {
            List<CartItemEntity> snapshot = cartDao.getCartItemsSync();
            cartDao.deleteByCartKey(item.productId, normalizedVariant(item.variantId));

            if (!isLoggedIn()) {
                postSuccess(callback);
                return;
            }

            try {
                Response<CartResponse> response = syncCurrentCart();
                handleRemoteMutation(response, snapshot, callback);
            } catch (IOException e) {
                rollback(snapshot, callback, "Không thể xóa sản phẩm. Vui lòng thử lại.");
            }
        });
    }

    @Override
    public void syncCartAfterLogin(CartCallback callback) {
        executor.execute(() -> {
            if (!isLoggedIn()) {
                postSuccess(callback);
                return;
            }
            try {
                List<CartItemEntity> localItems = cartDao.getCartItemsSync();
                List<CartItemEntity> serverItems = fetchServerCart();
                List<CartItem> mergedDomain = mergeUseCase.merge(toDomain(localItems), toDomain(serverItems));
                List<CartItemEntity> mergedEntities = materializeMerged(mergedDomain, localItems, serverItems);
                Response<CartResponse> syncResponse = apiService.syncCartToServer(
                        new SyncCartRequest(toSyncItems(mergedEntities))
                ).execute();
                if (syncResponse.isSuccessful()) {
                    List<CartItemEntity> responseItems = fromResponse(syncResponse.body());
                    cartDao.replaceCart(responseItems.isEmpty() ? markSynced(mergedEntities) : responseItems);
                    postSuccess(callback);
                    return;
                }
                postError(callback, "Chưa thể đồng bộ giỏ hàng từ máy chủ.");
            } catch (IOException e) {
                postError(callback, "Không thể kết nối máy chủ để đồng bộ giỏ hàng.");
            }
        });
    }

    private void handleRemoteMutation(Response<CartResponse> response,
                                      List<CartItemEntity> snapshot,
                                      CartCallback callback) {
        if (response.isSuccessful()) {
            List<CartItemEntity> responseItems = fromResponse(response.body());
            if (!responseItems.isEmpty()) {
                cartDao.replaceCart(responseItems);
            }
            postSuccess(callback);
            return;
        }
        rollback(snapshot, callback, "Máy chủ chưa cập nhật được giỏ hàng.");
    }

    private List<CartItemEntity> fetchServerCart() throws IOException {
        // TODO(MOB-017 backend): confirm the authenticated GET /cart contract.
        // Without a real fetch endpoint, cross-device cart restore cannot be guaranteed.
        Response<CartResponse> response = apiService.getCart().execute();
        if (response.isSuccessful()) {
            return fromResponse(response.body());
        }
        return new ArrayList<>();
    }

    private Response<CartResponse> syncCurrentCart() throws IOException {
        List<CartItemEntity> currentItems = cartDao.getCartItemsSync();
        return apiService.syncCartToServer(new SyncCartRequest(toSyncItems(currentItems))).execute();
    }

    private boolean isLoggedIn() {
        return tokenManager.isLoggedIn();
    }

    private CartItemEntity toCartEntity(ProductEntity product, int quantity) {
        CartItemEntity item = new CartItemEntity();
        item.productId = product._id;
        item.variantId = "";
        item.name = product.name;
        item.specs = product.specs;
        item.imageUrl = product.images;
        item.unitPrice = product.salePrice != null ? product.salePrice : product.price;
        item.quantity = quantity;
        item.updatedAt = System.currentTimeMillis();
        return item;
    }

    private List<CartItemEntity> fromResponse(@Nullable CartResponse response) {
        List<CartItemEntity> entities = new ArrayList<>();
        if (response == null) return entities;
        for (CartResponse.Item item : response.getItems()) {
            if (item == null || item.productId == null || item.productId.trim().isEmpty()) {
                continue;
            }
            CartItemEntity entity = new CartItemEntity();
            entity.productId = item.productId;
            entity.variantId = normalizedVariant(item.variantId);
            entity.name = firstNonEmpty(item.name, item.productName);
            entity.specs = item.specs;
            entity.imageUrl = firstNonEmpty(item.imageUrl, item.image);
            entity.unitPrice = item.unitPrice > 0 ? item.unitPrice : item.price;
            entity.quantity = Math.max(1, item.quantity);
            entity.synced = true;
            entity.updatedAt = System.currentTimeMillis();
            entities.add(entity);
        }
        return entities;
    }

    private List<CartItem> toDomain(List<CartItemEntity> entities) {
        List<CartItem> items = new ArrayList<>();
        if (entities == null) return items;
        for (CartItemEntity entity : entities) {
            if (entity != null) {
            items.add(new CartItem(entity.productId, normalizedVariant(entity.variantId), entity.quantity));
            }
        }
        return items;
    }

    private List<CartItemEntity> materializeMerged(List<CartItem> merged,
                                                   List<CartItemEntity> localItems,
                                                   List<CartItemEntity> serverItems) {
        List<CartItemEntity> result = new ArrayList<>();
        for (CartItem item : merged) {
            CartItemEntity source = findByKey(localItems, item);
            if (source == null) {
                source = findByKey(serverItems, item);
            }
            CartItemEntity entity = copyOf(source);
            entity.productId = item.productId;
            entity.variantId = normalizedVariant(item.variantId);
            entity.quantity = item.quantity;
            entity.synced = true;
            entity.updatedAt = System.currentTimeMillis();
            result.add(entity);
        }
        return result;
    }

    private CartItemEntity findByKey(List<CartItemEntity> entities, CartItem item) {
        if (entities == null) return null;
        for (CartItemEntity entity : entities) {
            if (entity != null
                    && safeEquals(entity.productId, item.productId)
                    && safeEquals(normalizedVariant(entity.variantId), normalizedVariant(item.variantId))) {
                return entity;
            }
        }
        return null;
    }

    private CartItemEntity copyOf(@Nullable CartItemEntity source) {
        CartItemEntity copy = new CartItemEntity();
        if (source == null) return copy;
        copy.productId = source.productId;
        copy.variantId = source.variantId;
        copy.name = source.name;
        copy.specs = source.specs;
        copy.imageUrl = source.imageUrl;
        copy.unitPrice = source.unitPrice;
        copy.quantity = source.quantity;
        copy.synced = source.synced;
        copy.updatedAt = source.updatedAt;
        return copy;
    }

    private List<CartItemEntity> markSynced(List<CartItemEntity> items) {
        for (CartItemEntity item : items) {
            item.synced = true;
            item.updatedAt = System.currentTimeMillis();
        }
        return items;
    }

    private List<SyncCartRequest.Item> toSyncItems(List<CartItemEntity> entities) {
        List<SyncCartRequest.Item> items = new ArrayList<>();
        for (CartItemEntity entity : entities) {
            items.add(new SyncCartRequest.Item(entity.productId, normalizedVariant(entity.variantId), entity.quantity));
        }
        return items;
    }

    private void rollback(List<CartItemEntity> snapshot, CartCallback callback, String message) {
        cartDao.replaceCart(snapshot);
        postError(callback, message);
    }

    private void postSuccess(CartCallback callback) {
        if (callback != null) {
            mainHandler.post(callback::onSuccess);
        }
    }

    private void postError(CartCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }

    private boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.isEmpty()) return first;
        return second;
    }

    private String normalizedVariant(String variantId) {
        return variantId == null ? "" : variantId;
    }
}
