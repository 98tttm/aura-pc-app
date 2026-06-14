package com.example.aura_pc_app.data.repository;

import com.example.aura_pc_app.domain.model.Review;
import com.example.aura_pc_app.domain.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MockReviewRepository implements ReviewRepository {
    private static final String RESOURCE_PREFIX =
            "android.resource://com.example.aura_pc_app/drawable/";
    private static MockReviewRepository instance;

    private final Map<String, List<Review>> reviewsByProduct = new HashMap<>();

    public static synchronized MockReviewRepository getInstance() {
        if (instance == null) {
            instance = new MockReviewRepository();
        }
        return instance;
    }

    private MockReviewRepository() {
    }

    @Override
    public synchronized List<Review> getReviews(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!reviewsByProduct.containsKey(productId)) {
            reviewsByProduct.put(productId, createMockReviews(productId));
        }
        return new ArrayList<>(reviewsByProduct.get(productId));
    }

    @Override
    public synchronized void addReview(Review review) {
        if (review == null || review.getProductId() == null) {
            return;
        }
        List<Review> reviews = reviewsByProduct.computeIfAbsent(
                review.getProductId(), this::createMockReviews);
        reviews.add(0, review);
    }

    @Override
    public boolean isDeliveredPurchaseEligible(String productId) {
        // TODO: Replace this demo rule with a backend endpoint that verifies the
        // authenticated user bought this product and the order status is delivered.
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        String normalized = productId.toLowerCase(Locale.ROOT);
        return !normalized.contains("monitor") && !normalized.contains("keyboard");
    }

    private List<Review> createMockReviews(String productId) {
        String normalized = productId.toLowerCase(Locale.ROOT);
        if (normalized.contains("headset")) {
            return new ArrayList<>();
        }

        long now = System.currentTimeMillis();
        List<Review> reviews = new ArrayList<>();
        reviews.add(new Review(
                productId + "-review-1",
                productId,
                "Minh Anh",
                5,
                "San pham dung nhu mo ta, dong goi ky va hieu nang rat tot.",
                Arrays.asList(
                        RESOURCE_PREFIX + "pc_main_1",
                        RESOURCE_PREFIX + "pc_main_2"
                ),
                now - 2L * 24 * 60 * 60 * 1000,
                true,
                new Review.AdminReply(
                        "AuraPC cam on ban da tin tuong va chia se trai nghiem.",
                        now - 24L * 60 * 60 * 1000
                )
        ));
        reviews.add(new Review(
                productId + "-review-2",
                productId,
                "Hoang Nam",
                Math.abs(productId.hashCode()) % 2 == 0 ? 4 : 5,
                "May chay on dinh, giao hang nhanh. Minh se tiep tuc su dung them.",
                Collections.singletonList(RESOURCE_PREFIX + "pc_main_3"),
                now - 8L * 24 * 60 * 60 * 1000,
                true,
                null
        ));
        reviews.add(new Review(
                productId + "-review-3",
                productId,
                "Thu Ha",
                4,
                "Trai nghiem tong the tot, tu van nhiet tinh.",
                Collections.emptyList(),
                now - 18L * 24 * 60 * 60 * 1000,
                true,
                new Review.AdminReply(
                        "Cam on ban. AuraPC se tiep tuc cai thien chat luong dich vu.",
                        now - 16L * 24 * 60 * 60 * 1000
                )
        ));
        return reviews;
    }
}
