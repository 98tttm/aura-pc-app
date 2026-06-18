package com.aura.pc.ui.orders;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderRepository {
    private static final String TAG = "OrderRepository";
    private static final String PREFS_NAME = "order_history_prefs";
    private static final String ORDER_NAME_PREFIX = "order_name_";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();
    private static final Type LIST_TYPE = new TypeToken<List<Object>>() { }.getType();

    public interface Callback2 {
        void onSuccess(List<OrderHistoryItem> orders);
        void onError(String message);
    }

    public interface RenameCallback {
        void onSuccess();
        void onError(String message);
    }

    private final ApiService api;
    private final SharedPreferences prefs;

    public OrderRepository(Context context) {
        Context appContext = context.getApplicationContext();
        api = ApiClient.getInstance(appContext).getApiService();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void load(final Callback2 callback) {
        api.getMyOrders().enqueue(new ObjectCallback(callback, () ->
                api.getCurrentUserOrders().enqueue(new ObjectCallback(callback, () ->
                        api.getOrders().enqueue(new ObjectCallback(callback, () ->
                                loadFromUser(callback)))))));
    }

    public void updateOrderName(OrderHistoryItem order, String name, RenameCallback callback) {
        String key = orderKey(order);
        if (TextUtils.isEmpty(key)) {
            callback.onError("Không tìm thấy mã đơn để cập nhật");
            return;
        }
        String safeName = name == null ? "" : name.trim();
        if (TextUtils.isEmpty(safeName)) {
            callback.onError("Tên đơn hàng không được để trống");
            return;
        }
        prefs.edit().putString(key, safeName).apply();
        order.name = safeName;
        callback.onSuccess();
        if (TextUtils.isEmpty(order.id)) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("name", safeName);
        body.put("title", safeName);
        body.put("orderName", safeName);
        body.put("order_name", safeName);
        api.updateOrder(order.id, body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) {
                    return;
                }
                Log.d(TAG, "server did not persist order name: " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                Log.e(TAG, "rename order failed", t);
            }
        });
    }

    private void loadFromUser(final Callback2 callback) {
        api.getCurrentUser().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!response.isSuccessful()) {
                    callback.onError("Không tải được đơn hàng (" + response.code() + ")");
                    return;
                }
                List<OrderHistoryItem> orders = parseOrders(response.body());
                callback.onSuccess(orders);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Log.e(TAG, "load user orders failed", t);
                callback.onError("Không kết nối được máy chủ. Kiểm tra mạng.");
            }
        });
    }

    private class ObjectCallback implements Callback<Object> {
        private final Callback2 callback;
        private final Runnable fallback;

        ObjectCallback(Callback2 callback, Runnable fallback) {
            this.callback = callback;
            this.fallback = fallback;
        }

        @Override
        public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
            if (response.isSuccessful()) {
                List<OrderHistoryItem> orders = parseOrders(response.body());
                if (!orders.isEmpty()) {
                    callback.onSuccess(orders);
                    return;
                }
            }
            fallback.run();
        }

        @Override
        public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
            Log.d(TAG, "order endpoint fallback: " + call.request().url(), t);
            fallback.run();
        }
    }

    @SuppressWarnings("unchecked")
    private List<OrderHistoryItem> parseOrders(Object body) {
        Object root = normalizeJson(body);
        List<Object> rawOrders = findFirstList(root,
                "orders", "orderHistory", "order_history", "purchaseHistory",
                "purchase_history", "purchases", "items", "data", "results");
        if (rawOrders == null) {
            return new ArrayList<>();
        }
        List<OrderHistoryItem> orders = new ArrayList<>();
        for (Object rawOrder : rawOrders) {
            if (!(rawOrder instanceof Map)) {
                continue;
            }
            OrderHistoryItem order = parseOrder((Map<String, Object>) rawOrder);
            applyLocalName(order);
            if (!TextUtils.isEmpty(order.id) || !TextUtils.isEmpty(order.code) || !order.products.isEmpty()) {
                orders.add(order);
            }
        }
        return orders;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map || value instanceof List) {
            String json = GSON.toJson(value);
            return value instanceof List
                    ? GSON.fromJson(json, LIST_TYPE)
                    : GSON.fromJson(json, MAP_TYPE);
        }
        return value;
    }

    private OrderHistoryItem parseOrder(Map<String, Object> map) {
        OrderHistoryItem order = new OrderHistoryItem();
        order.id = firstString(map, "_id", "id", "orderId", "order_id");
        order.code = firstString(map, "code", "orderCode", "order_code", "orderNo", "orderNumber", "number");
        order.name = firstString(map, "name", "title", "orderName", "order_name", "label");
        if (TextUtils.isEmpty(order.code) && !TextUtils.isEmpty(order.id)) {
            order.code = "#" + order.id;
        }
        order.createdAt = firstString(map, "createdAt", "created_at", "orderDate", "order_date", "date", "created");
        order.status = firstString(map, "status", "orderStatus", "order_status", "state");
        order.deliveryMethod = firstString(map, "deliveryMethod", "delivery_method", "shippingMethod", "shipping_method");
        order.total = firstNumber(map, "total", "totalAmount", "total_amount", "grandTotal", "grand_total", "finalTotal", "final_total", "amount");

        Object nested = firstValue(map, "items", "products", "orderItems", "order_items", "cartItems", "cart_items", "details");
        if (nested instanceof Collection) {
            for (Object item : (Collection<?>) nested) {
                OrderProduct product = parseProduct(item);
                if (!TextUtils.isEmpty(product.name) || product.price > 0) {
                    order.products.add(product);
                }
            }
        }
        if (order.total <= 0) {
            double sum = 0;
            for (OrderProduct product : order.products) {
                sum += product.price * Math.max(1, product.quantity);
            }
            order.total = sum;
        }
        return order;
    }

    private void applyLocalName(OrderHistoryItem order) {
        String key = orderKey(order);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        String localName = prefs.getString(key, "");
        if (!TextUtils.isEmpty(localName)) {
            order.name = localName;
        }
    }

    private String orderKey(OrderHistoryItem order) {
        if (order == null) {
            return "";
        }
        String source = !TextUtils.isEmpty(order.id) ? order.id : order.code;
        return TextUtils.isEmpty(source) ? "" : ORDER_NAME_PREFIX + source;
    }

    @SuppressWarnings("unchecked")
    private OrderProduct parseProduct(Object raw) {
        OrderProduct product = new OrderProduct();
        if (!(raw instanceof Map)) {
            return product;
        }
        Map<String, Object> item = (Map<String, Object>) raw;
        Object nested = firstValue(item, "product", "productId", "product_id", "sku");
        Map<String, Object> productMap = nested instanceof Map ? (Map<String, Object>) nested : new LinkedHashMap<>();

        product.id = firstNonEmpty(
                firstString(item, "productId", "product_id", "id", "_id"),
                firstString(productMap, "_id", "id", "productId", "product_id"));
        product.name = firstNonEmpty(
                firstString(item, "name", "title", "productName", "product_name"),
                firstString(productMap, "name", "title", "productName", "product_name"));
        product.imageUrl = absoluteImageUrl(firstNonEmpty(
                firstImage(item),
                firstImage(productMap),
                firstString(item, "image", "imageUrl", "thumbnail", "thumb"),
                firstString(productMap, "image", "imageUrl", "thumbnail", "thumb")));
        product.price = firstPositive(
                firstNumber(item, "price", "unitPrice", "unit_price", "salePrice", "sale_price", "finalPrice", "final_price"),
                firstNumber(productMap, "price", "unitPrice", "unit_price", "salePrice", "sale_price", "finalPrice", "final_price"));
        product.quantity = (int) Math.max(1, Math.round(firstPositive(
                firstNumber(item, "quantity", "qty", "count"),
                firstNumber(productMap, "quantity", "qty", "count"))));
        return product;
    }

    @SuppressWarnings("unchecked")
    private List<Object> findFirstList(Object value, String... keys) {
        if (value instanceof List) {
            return (List<Object>) value;
        }
        if (!(value instanceof Map)) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        for (String key : keys) {
            Object candidate = map.get(key);
            if (candidate instanceof List) {
                return (List<Object>) candidate;
            }
            List<Object> nested = findFirstList(candidate, keys);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private Object firstValue(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstString(Map<String, Object> map, String... keys) {
        Object value = firstValue(map, keys);
        if (value == null) {
            return "";
        }
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            return number == Math.rint(number) ? String.valueOf((long) number) : String.valueOf(number);
        }
        String text = String.valueOf(value).trim();
        return "null".equalsIgnoreCase(text) ? "" : text;
    }

    private double firstNumber(Map<String, Object> map, String... keys) {
        Object value = firstValue(map, keys);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(String.valueOf(value).replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private String firstImage(Map<String, Object> map) {
        Object images = firstValue(map, "images", "gallery", "photos");
        if (images instanceof List && !((List<?>) images).isEmpty()) {
            Object first = ((List<?>) images).get(0);
            if (first instanceof Map) {
                return firstString((Map<String, Object>) first, "url", "src", "imageUrl", "path");
            }
            return String.valueOf(first);
        }
        return "";
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private double firstPositive(double... values) {
        for (double value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private String absoluteImageUrl(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        String apiBase = Constants.BASE_URL;
        String hostBase = apiBase.endsWith("/api/")
                ? apiBase.substring(0, apiBase.length() - "/api/".length())
                : apiBase.replaceAll("/+$", "");
        if (trimmed.startsWith("/")) {
            return hostBase + trimmed;
        }
        return hostBase + "/" + trimmed;
    }

    public static String normalizeStatus(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String status = value.trim().toLowerCase(Locale.ROOT);
        if (status.contains("pending") || status.contains("confirm") || status.contains("chờ")) {
            return "pending";
        }
        if (status.contains("process") || status.contains("processing") || status.contains("xử")) {
            return "processing";
        }
        if (status.contains("done") || status.contains("complete") || status.contains("delivered") || status.contains("đã giao")) {
            return "delivered";
        }
        if (status.contains("ship") || status.contains("deliver") || status.contains("giao") || status.contains("vận")) {
            return "shipping";
        }
        if (status.contains("cancel") || status.contains("hủy")) {
            return "cancelled";
        }
        if (status.contains("return") || status.contains("trả")) {
            return "returned";
        }
        return status;
    }
}
