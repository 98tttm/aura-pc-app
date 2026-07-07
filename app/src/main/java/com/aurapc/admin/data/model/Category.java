package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Category implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("category_id")
    public String categoryId;

    @SerializedName("name")
    public String name;

    @SerializedName("slug")
    public String slug;

    @SerializedName("parent_id")
    public String parentId;

    @SerializedName("level")
    public Integer level;

    @SerializedName("image")
    public String image;

    @SerializedName("is_active")
    public Boolean active;

    @SerializedName("display_order")
    public Integer displayOrder;

    @SerializedName("product_count")
    public Integer productCount;

    @SerializedName("description")
    public String description;
}