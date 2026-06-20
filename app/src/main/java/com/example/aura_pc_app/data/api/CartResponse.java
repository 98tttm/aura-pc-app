package com.example.aura_pc_app.data.api;

import com.google.gson.annotations.SerializedName;

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
        public Product product;
        public String variantId;
        public String name;
        public String productName;
        public String specs;
        public String imageUrl;
        public String image;
        @SerializedName(value = "price", alternate = {"currentPrice", "current_price"})
        public double price;
        @SerializedName(value = "unitPrice", alternate = {"unit_price"})
        public double unitPrice;
        @SerializedName(value = "salePrice", alternate = {"sale_price", "final_price", "discountPrice", "discount_price"})
        public Double salePrice;
        public int quantity;
    }

    public static class Product {
        public String _id;
        public String id;
        public String name;
        public String specs;
        public String imageUrl;
        public String image;
        public Object images;
        @SerializedName(value = "price", alternate = {"currentPrice", "current_price"})
        public double price;
        @SerializedName(value = "unitPrice", alternate = {"unit_price"})
        public double unitPrice;
        @SerializedName(value = "salePrice", alternate = {"sale_price", "final_price", "discountPrice", "discount_price"})
        public Double salePrice;
    }
}
