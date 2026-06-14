package com.example.aura_pc_app.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Review {
    private final String id;
    private final String productId;
    private final String userName;
    private final int rating;
    private final String content;
    private final List<String> imageUris;
    private final long createdAt;
    private final boolean deliveredPurchase;
    private final AdminReply adminReply;

    public Review(
            String id,
            String productId,
            String userName,
            int rating,
            String content,
            List<String> imageUris,
            long createdAt,
            boolean deliveredPurchase,
            AdminReply adminReply
    ) {
        this.id = id;
        this.productId = productId;
        this.userName = userName;
        this.rating = Math.max(1, Math.min(5, rating));
        this.content = content;
        List<String> safeImages = imageUris == null ? Collections.emptyList() : imageUris;
        this.imageUris = Collections.unmodifiableList(
                new ArrayList<>(safeImages.subList(0, Math.min(3, safeImages.size())))
        );
        this.createdAt = createdAt;
        this.deliveredPurchase = deliveredPurchase;
        this.adminReply = adminReply;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getUserName() {
        return userName;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public List<String> getImageUris() {
        return imageUris;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isDeliveredPurchase() {
        return deliveredPurchase;
    }

    public AdminReply getAdminReply() {
        return adminReply;
    }

    public static class AdminReply {
        private final String content;
        private final long createdAt;

        public AdminReply(String content, long createdAt) {
            this.content = content;
            this.createdAt = createdAt;
        }

        public String getContent() {
            return content;
        }

        public long getCreatedAt() {
            return createdAt;
        }
    }
}
