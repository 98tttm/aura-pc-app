package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class HubComment implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("post")
    public String post;

    @SerializedName("author")
    public Object author;

    @SerializedName("content")
    public String content;

    @SerializedName("parentComment")
    public String parentComment;

    @SerializedName("replyCount")
    public Integer replyCount;

    @SerializedName("replies")
    public List<HubComment> replies;

    @SerializedName("createdAt")
    public String createdAt;

    public String authorName() {
        if (author instanceof java.util.Map) {
            Object username = ((java.util.Map<?, ?>) author).get("username");
            if (username != null) return String.valueOf(username);
            Object profile = ((java.util.Map<?, ?>) author).get("profile");
            if (profile instanceof java.util.Map) {
                Object fullName = ((java.util.Map<?, ?>) profile).get("fullName");
                if (fullName != null) return String.valueOf(fullName);
            }
        }
        return "Người dùng";
    }
}