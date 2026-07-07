package com.aurapc.admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class HubPost implements Serializable {

    @SerializedName(value = "_id", alternate = {"id"})
    public String id;

    @SerializedName("author")
    public Object author;

    @SerializedName("content")
    public String content;

    @SerializedName("topic")
    public String topic;

    @SerializedName("status")
    public String status;

    @SerializedName("isPublished")
    public Boolean isPublished;

    @SerializedName("scheduledAt")
    public String scheduledAt;

    @SerializedName("reviewedBy")
    public String reviewedBy;

    @SerializedName("reviewedAt")
    public String reviewedAt;

    @SerializedName("rejectedReason")
    public String rejectedReason;

    @SerializedName("images")
    public List<String> images;

    @SerializedName("likeCount")
    public Integer likeCount;

    @SerializedName("commentCount")
    public Integer commentCount;

    @SerializedName("repostCount")
    public Integer repostCount;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedAt")
    public String updatedAt;

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
        return "Tác giả";
    }

    public String authorAvatar() {
        if (author instanceof java.util.Map) {
            Object avatar = ((java.util.Map<?, ?>) author).get("avatar");
            if (avatar != null) return String.valueOf(avatar);
        }
        return null;
    }
}