package com.example.aura_pc_app.domain.cart;

public class CartItem {
    public final String productId;
    public final String variantId;
    public final int quantity;

    public CartItem(String productId, String variantId, int quantity) {
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
    }

    public String key() {
        return productId + "::" + (variantId == null ? "" : variantId);
    }
}
