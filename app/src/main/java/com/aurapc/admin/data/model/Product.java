package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Product {
    @SerializedName("_id")
    public String id;
    public String name;
    public String slug;
    public String brand;
    public String sku;
    public String description;
    public String shortDescription;
    public Double price;
    public Double salePrice;
    public Integer stock;
    public Integer stockLimit;
    public Boolean isActive;
    public Boolean active;
    public Boolean isFeatured;
    public Boolean featured;
    public String thumbnail;
    public List<Object> images;
    public List<String> category_ids;
    public String category_id;
    public Integer primaryCategoryId;
    public List<Integer> categoryIds;
    public Map<String, Object> category;
    public List<Map<String, Object>> variants;
    public Map<String, Object> specs;
    public Map<String, Object> techSpecs;
    public Integer warrantyMonths;
    public String createdAt;
    public String updatedAt;

    public boolean active() {
        if (isActive != null) return isActive;
        if (active != null) return active;
        return true;
    }

    public List<String> imageUrls() {
        List<String> urls = new ArrayList<>();
        if (images == null) return urls;
        for (Object item : images) {
            String url = imageUrlFrom(item);
            if (url != null && !url.isEmpty()) urls.add(url);
        }
        return urls;
    }

    public String primaryImageUrl() {
        if (thumbnail != null && !thumbnail.isEmpty()) return thumbnail;
        List<String> urls = imageUrls();
        return urls.isEmpty() ? null : urls.get(0);
    }

    private String imageUrlFrom(Object item) {
        if (item == null) return null;
        if (item instanceof String) return (String) item;
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item;
            Object url = map.get("url");
            if (url == null) url = map.get("src");
            if (url == null) url = map.get("image");
            if (url == null) url = map.get("secure_url");
            return url != null ? url.toString() : null;
        }
        return item.toString();
    }
}
