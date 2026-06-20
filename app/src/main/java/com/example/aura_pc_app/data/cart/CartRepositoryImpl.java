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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final Set<String> priceRefreshInFlight = Collections.synchronizedSet(new HashSet<>());

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

    public void refreshMissingPrices(List<CartItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        executor.execute(() -> {
            for (CartItemEntity item : items) {
                if (!shouldRefreshPrice(item)) {
                    continue;
                }
                String productId = item.productId.trim();
                if (!priceRefreshInFlight.add(productId)) {
                    continue;
                }
                try {
                    Response<Map<String, Object>> response = apiService.getProductById(productId).execute();
                    if (!response.isSuccessful() || response.body() == null) {
                        continue;
                    }
                    CartItemEntity hydrated = hydrateFromProductMap(item, response.body());
                    if (hydrated.unitPrice > 0) {
                        cartDao.insertCartItem(hydrated);
                    }
                } catch (IOException ignored) {
                } finally {
                    priceRefreshInFlight.remove(productId);
                }
            }
        });
    }

    @Override
    public void addProduct(ProductEntity product, int quantity, CartCallback callback) {
        if (product == null || product._id == null || product._id.trim().isEmpty()) {
            postError(callback, "Không thể thêm sản phẩm này vào giỏ hàng");
            return;
        }
        executor.execute(() -> {
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
                handleRemoteMutation(syncCurrentCart(), callback);
            } catch (IOException e) {
                postSuccess(callback);
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
            cartDao.updateQuantity(item.productId, normalizedVariant(item.variantId), quantity, !isLoggedIn(), System.currentTimeMillis());

            if (!isLoggedIn()) {
                postSuccess(callback);
                return;
            }

            try {
                handleRemoteMutation(syncCurrentCart(), callback);
            } catch (IOException e) {
                postSuccess(callback);
            }
        });
    }

    @Override
    public void removeItem(CartItemEntity item, CartCallback callback) {
        if (item == null || item.productId == null) return;
        executor.execute(() -> {
            cartDao.deleteByCartKey(item.productId, normalizedVariant(item.variantId));

            if (!isLoggedIn()) {
                postSuccess(callback);
                return;
            }

            try {
                handleRemoteMutation(syncCurrentCart(), callback);
            } catch (IOException e) {
                postSuccess(callback);
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
                    cartDao.replaceCart(responseItems.isEmpty()
                            ? markSynced(mergedEntities)
                            : enrichResponseItems(responseItems, mergedEntities));
                    postSuccess(callback);
                    return;
                }
                cartDao.replaceCart(mergedEntities);
                postSuccess(callback);
            } catch (IOException e) {
                postSuccess(callback);
            }
        });
    }

    private void handleRemoteMutation(Response<CartResponse> response, CartCallback callback) {
        if (response.isSuccessful()) {
            List<CartItemEntity> responseItems = fromResponse(response.body());
            if (!responseItems.isEmpty()) {
                cartDao.replaceCart(enrichResponseItems(responseItems, cartDao.getCartItemsSync()));
            }
        }
        postSuccess(callback);
    }

    private List<CartItemEntity> fetchServerCart() throws IOException {
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
        item.unitPrice = product.salePrice != null && product.salePrice > 0 ? product.salePrice : product.price;
        item.quantity = quantity;
        item.updatedAt = System.currentTimeMillis();
        return item;
    }

    private List<CartItemEntity> fromResponse(@Nullable CartResponse response) {
        List<CartItemEntity> entities = new ArrayList<>();
        if (response == null) return entities;
        for (CartResponse.Item item : response.getItems()) {
            String productId = resolveProductId(item);
            if (productId.isEmpty()) {
                continue;
            }
            CartItemEntity entity = new CartItemEntity();
            entity.productId = productId;
            entity.variantId = normalizedVariant(item.variantId);
            entity.name = firstNonEmpty(firstNonEmpty(item.name, item.productName), item.product == null ? null : item.product.name);
            entity.specs = firstNonEmpty(item.specs, item.product == null ? null : item.product.specs);
            entity.imageUrl = firstNonEmpty(firstNonEmpty(item.imageUrl, item.image), firstProductImage(item.product));
            entity.unitPrice = resolveUnitPrice(item);
            entity.quantity = Math.max(1, item.quantity);
            entity.synced = true;
            entity.updatedAt = System.currentTimeMillis();
            entities.add(entity);
        }
        return entities;
    }

    private String resolveProductId(CartResponse.Item item) {
        if (item == null) return "";
        if (item.productId != null && !item.productId.trim().isEmpty()) {
            return item.productId.trim();
        }
        if (item.product != null && item.product._id != null && !item.product._id.trim().isEmpty()) {
            return item.product._id.trim();
        }
        if (item.product != null && item.product.id != null && !item.product.id.trim().isEmpty()) {
            return item.product.id.trim();
        }
        return "";
    }

    private double resolveUnitPrice(CartResponse.Item item) {
        if (item == null) return 0;
        if (item.unitPrice > 0) return item.unitPrice;
        if (item.price > 0) return item.price;
        if (item.product != null) {
            if (item.product.unitPrice > 0) return item.product.unitPrice;
            if (item.product.salePrice != null && item.product.salePrice > 0) return item.product.salePrice;
            if (item.product.price > 0) return item.product.price;
        }
        return 0;
    }

    private String firstProductImage(CartResponse.Product product) {
        if (product == null) return null;
        String direct = firstNonEmpty(product.imageUrl, product.image);
        if (direct != null && !direct.isEmpty()) return direct;
        return product.images == null ? null : String.valueOf(product.images);
    }

    private List<CartItemEntity> enrichResponseItems(List<CartItemEntity> responseItems,
                                                     List<CartItemEntity> fallbackItems) {
        if (responseItems == null || responseItems.isEmpty()) {
            return responseItems;
        }
        for (CartItemEntity responseItem : responseItems) {
            CartItemEntity fallback = findByCartKey(fallbackItems, responseItem.productId, responseItem.variantId);
            if (fallback == null) {
                fallback = findByProductId(fallbackItems, responseItem.productId);
            }
            if (fallback == null) {
                continue;
            }
            if (responseItem.unitPrice <= 0 && fallback.unitPrice > 0) {
                responseItem.unitPrice = fallback.unitPrice;
            }
            responseItem.name = firstNonEmpty(responseItem.name, fallback.name);
            responseItem.specs = firstNonEmpty(responseItem.specs, fallback.specs);
            responseItem.imageUrl = firstNonEmpty(responseItem.imageUrl, fallback.imageUrl);
        }
        return responseItems;
    }

    private CartItemEntity findByCartKey(List<CartItemEntity> items, String productId, String variantId) {
        if (items == null) return null;
        String normalizedVariant = normalizedVariant(variantId);
        for (CartItemEntity item : items) {
            if (item != null
                    && safeEquals(item.productId, productId)
                    && safeEquals(normalizedVariant(item.variantId), normalizedVariant)) {
                return item;
            }
        }
        return null;
    }

    private CartItemEntity findByProductId(List<CartItemEntity> items, String productId) {
        if (items == null) return null;
        for (CartItemEntity item : items) {
            if (item != null && safeEquals(item.productId, productId)) {
                return item;
            }
        }
        return null;
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

    private boolean shouldRefreshPrice(CartItemEntity item) {
        return item != null
                && item.unitPrice <= 0
                && item.productId != null
                && !item.productId.trim().isEmpty();
    }

    private CartItemEntity hydrateFromProductMap(CartItemEntity source, Map<String, Object> responseBody) {
        CartItemEntity hydrated = copyOf(source);
        Map<String, Object> product = unwrapProductMap(responseBody);
        hydrated.unitPrice = firstNumber(product,
                "salePrice", "sale_price", "final_price",
                "discountPrice", "discount_price",
                "currentPrice", "current_price",
                "price", "unitPrice", "unit_price");
        hydrated.name = firstNonEmpty(hydrated.name, firstString(product, "name", "productName", "title"));
        hydrated.specs = firstNonEmpty(hydrated.specs, rawString(firstNonNull(product, "specs", "shortSpecs", "description")));
        hydrated.imageUrl = firstNonEmpty(hydrated.imageUrl, rawString(firstNonNull(product, "images", "imageUrl", "image", "thumbnail")));
        hydrated.updatedAt = System.currentTimeMillis();
        return hydrated;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapProductMap(Map<String, Object> body) {
        Object data = body.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        Object product = body.get("product");
        if (product instanceof Map) {
            return (Map<String, Object>) product;
        }
        return body;
    }

    private Object firstNonNull(Map<String, Object> data, String... keys) {
        if (data == null) return null;
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstString(Map<String, Object> data, String... keys) {
        if (data == null) return "";
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return ((String) value).trim();
            }
            if (value instanceof Number) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private double firstNumber(Map<String, Object> data, String... keys) {
        if (data == null) return 0;
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble(((String) value).replace(".", "").replace(",", "").trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private String rawString(Object value) {
        return value == null ? null : String.valueOf(value);
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
