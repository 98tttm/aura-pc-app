package com.example.aura_pc_app.data.api;

import java.util.List;

public class SyncCartRequest {
    public List<Item> items;

    public SyncCartRequest(List<Item> items) {
        this.items = items;
    }

    public static class Item {
        public String productId;
        public String variantId;
        public int quantity;

        public Item(String productId, int quantity) {
            this(productId, null, quantity);
        }

        public Item(String productId, String variantId, int quantity) {
            this.productId = productId;
            this.variantId = variantId;
            this.quantity = quantity;
        }
    }
}
