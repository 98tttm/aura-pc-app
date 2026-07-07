package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class BlogPost implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("title")
    public String title;

    @SerializedName("slug")
    public String slug;

    @SerializedName("excerpt")
    public String excerpt;

    @SerializedName("content")
    public String content;

    @SerializedName("coverImage")
    public String coverImage;

    @SerializedName("category")
    public String category;

    @SerializedName("tags")
    public List<String> tags;

    @SerializedName("author")
    public String author;

    @SerializedName("status")
    public String status;

    @SerializedName("publishedAt")
    public String publishedAt;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedAt")
    public String updatedAt;

    @SerializedName("views")
    public Integer views;
}