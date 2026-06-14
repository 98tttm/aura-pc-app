package com.example.aura_pc_app.data.api;

import java.util.ArrayList;
import java.util.List;

public class CartResponse {
    public boolean success;
    public String message;
    public List<Item> items;
    public Cart cart;

    public List<Item> getItems() {
        if (items != null) {
            return items;
        }
        if (cart != null && cart.items != null) {
            return cart.items;
        }
        return new ArrayList<>();
    }

    public static class Cart {
        public List<Item> items;
    }

    public static class Item {
        public String productId;
        public String variantId;
        public String name;
        public String productName;
        public String specs;
        public String imageUrl;
        public String image;
        public double price;
        public double unitPrice;
        public int quantity;
    }
}
