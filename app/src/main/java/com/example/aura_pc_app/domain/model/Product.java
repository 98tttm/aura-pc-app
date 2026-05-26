package com.example.aura_pc_app.domain.model;

import java.util.List;

public class Product {
    private String name;
    private String description;
    private String currentPrice;
    private String oldPrice;
    private String discount;
    private float rating;
    private int reviewCount;
    private int soldCount;
    private List<Integer> images;
    private List<ProductSpec> specs;

    public Product(String name, String description, String currentPrice, String oldPrice, String discount, float rating, int reviewCount, int soldCount, List<Integer> images, List<ProductSpec> specs) {
        this.name = name;
        this.description = description;
        this.currentPrice = currentPrice;
        this.oldPrice = oldPrice;
        this.discount = discount;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.soldCount = soldCount;
        this.images = images;
        this.specs = specs;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCurrentPrice() { return currentPrice; }
    public String getOldPrice() { return oldPrice; }
    public String getDiscount() { return discount; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public int getSoldCount() { return soldCount; }
    public List<Integer> getImages() { return images; }
    public List<ProductSpec> getSpecs() { return specs; }
}
