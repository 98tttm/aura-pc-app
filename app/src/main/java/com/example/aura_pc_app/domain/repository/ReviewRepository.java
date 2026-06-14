package com.example.aura_pc_app.domain.repository;

import com.example.aura_pc_app.domain.model.Review;

import java.util.List;

public interface ReviewRepository {
    List<Review> getReviews(String productId);

    void addReview(Review review);

    boolean isDeliveredPurchaseEligible(String productId);
}
