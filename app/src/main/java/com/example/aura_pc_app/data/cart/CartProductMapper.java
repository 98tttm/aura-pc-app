package com.example.aura_pc_app.data.cart;

import android.text.TextUtils;

import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.google.gson.Gson;

import java.util.Map;

public final class CartProductMapper {
    private static final Gson GSON = new Gson();

    private CartProductMapper() {
    }

    public static ProductEntity fromApiMap(Map<String, Object> product) {
        if (product == null) {
            return null;
        }

        String id = firstString(product, "_id", "id", "product_id", "productId");
        if (TextUtils.isEmpty(id)) {
            return null;
        }

        ProductEntity entity = new ProductEntity();
        entity._id = id;
        entity.name = firstString(product, "name", "productName", "title");
        entity.price = firstNumber(product,
                "price", "sale_price", "salePrice", "final_price",
                "currentPrice", "current_price", "discountPrice", "discount_price");
        double oldPrice = firstNumber(product, "old_price", "original_price", "compare_at_price", "market_price");
        if (oldPrice > 0) {
            entity.oldPrice = oldPrice;
        }
        entity.salePrice = firstNullableNumber(product,
                "salePrice", "sale_price", "final_price",
                "currentPrice", "current_price", "discountPrice", "discount_price");
        entity.images = rawJsonOrString(firstNonNull(product, "images", "imageUrl", "image", "thumbnail"));
        entity.specs = rawJsonOrString(firstNonNull(product, "specs", "shortSpecs", "description"));
        entity.brand = firstString(product, "brand");
        entity.active = true;
        return entity;
    }

    private static Object firstNonNull(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstString(Map<String, Object> data, String... keys) {
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

    private static double firstNumber(Map<String, Object> data, String... keys) {
        Double value = firstNullableNumber(data, keys);
        return value == null ? 0 : value;
    }

    private static Double firstNullableNumber(Map<String, Object> data, String... keys) {
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
        return null;
    }

    private static String rawJsonOrString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return GSON.toJson(value);
    }
}
