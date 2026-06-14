package com.example.aura_pc_app.domain.cart;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CartMergeUseCase {
    public List<CartItem> merge(List<CartItem> localItems, List<CartItem> serverItems) {
        Map<String, CartItem> merged = new LinkedHashMap<>();
        addAll(merged, serverItems);
        addAll(merged, localItems);
        return new ArrayList<>(merged.values());
    }

    private void addAll(Map<String, CartItem> merged, List<CartItem> items) {
        if (items == null) return;
        for (CartItem item : items) {
            if (item == null || item.productId == null || item.productId.trim().isEmpty()) {
                continue;
            }
            String key = item.key();
            CartItem existing = merged.get(key);
            int quantity = Math.max(1, item.quantity);
            if (existing != null) {
                quantity += Math.max(1, existing.quantity);
            }
            merged.put(key, new CartItem(item.productId, item.variantId, quantity));
        }
    }
}
