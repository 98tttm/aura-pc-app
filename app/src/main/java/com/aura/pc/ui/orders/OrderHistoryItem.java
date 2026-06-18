package com.aura.pc.ui.orders;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryItem {
    public String id;
    public String code;
    public String name;
    public String createdAt;
    public String status;
    public String deliveryMethod;
    public double total;
    public final List<OrderProduct> products = new ArrayList<>();

    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String normalized = query.trim().toLowerCase();
        if (contains(name, normalized) || contains(code, normalized) || contains(id, normalized)) {
            return true;
        }
        for (OrderProduct product : products) {
            if (product != null && contains(product.name, normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

}
