package com.example.aura_pc_app.data.api;

import com.google.gson.JsonElement;
import java.util.List;

public class ProductResponse {
    public List<Item> items;
    public int total;

    public static class Item {
        public String _id;
        public String product_id;
        public String handle;
        public String slug;
        public String name;
        public double price;
        public Double old_price;
        public Double salePrice;
        public JsonElement images;
        public String category_id;
        public List<String> category_ids;
        public JsonElement specs;
        public String brand;
        public int stock;
        public Boolean active;
    }
}
