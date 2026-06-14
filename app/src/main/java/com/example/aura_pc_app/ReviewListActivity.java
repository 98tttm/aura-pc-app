package com.example.aura_pc_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.adapter.ReviewAdapter;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.data.repository.MockReviewRepository;
import com.example.aura_pc_app.domain.model.Review;
import com.example.aura_pc_app.domain.repository.ReviewRepository;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewListActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "review_product_id";
    public static final String EXTRA_PRODUCT_NAME = "review_product_name";

    private final List<Review> allReviews = new ArrayList<>();
    private ReviewRepository repository;
    private ReviewAdapter adapter;
    private RecyclerView recycler;
    private TextView empty;
    private EditText search;
    private TextView overallRating;
    private TextView reviewCount;
    private String productId;
    private String productName;
    private Filter filter = Filter.ALL;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_list);

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productName = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        repository = MockReviewRepository.getInstance();

        recycler = findViewById(R.id.reviewListRecycler);
        empty = findViewById(R.id.reviewListEmpty);
        search = findViewById(R.id.reviewSearchInput);
        overallRating = findViewById(R.id.reviewListOverallRating);
        reviewCount = findViewById(R.id.reviewListCount);
        adapter = new ReviewAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.reviewListBack).setOnClickListener(v -> finish());
        findViewById(R.id.reviewListWriteButton).setOnClickListener(v -> openWriteReview());
        setupFilters();
        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReviews();
    }

    private void setupFilters() {
        ChipGroup group = findViewById(R.id.reviewFilterGroup);
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.reviewFilterImages) {
                filter = Filter.IMAGES;
            } else if (id == R.id.reviewFilterFiveStar) {
                filter = Filter.FIVE_STAR;
            } else if (id == R.id.reviewSortHighest) {
                filter = Filter.HIGHEST;
            } else if (id == R.id.reviewSortLowest) {
                filter = Filter.LOWEST;
            } else {
                filter = Filter.ALL;
            }
            render();
        });
    }

    private void setupSearch() {
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                render();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
    }

    private void loadReviews() {
        allReviews.clear();
        allReviews.addAll(repository.getReviews(productId));
        updateSummary();
        render();
    }

    private void updateSummary() {
        double average = 0;
        for (Review review : allReviews) {
            average += review.getRating();
        }
        if (!allReviews.isEmpty()) {
            average /= allReviews.size();
        }
        overallRating.setText(getString(
                R.string.review_rating_out_of_five, String.format(Locale.US, "%.1f", average)));
        reviewCount.setText(getString(R.string.review_total_count, allReviews.size()));
    }

    private void render() {
        String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Review> visible = new ArrayList<>();
        for (Review review : allReviews) {
            boolean matchesSearch = query.isEmpty()
                    || review.getUserName().toLowerCase(Locale.ROOT).contains(query)
                    || review.getContent().toLowerCase(Locale.ROOT).contains(query);
            boolean matchesFilter = filter != Filter.IMAGES || !review.getImageUris().isEmpty();
            matchesFilter &= filter != Filter.FIVE_STAR || review.getRating() == 5;
            if (matchesSearch && matchesFilter) {
                visible.add(review);
            }
        }
        if (filter == Filter.HIGHEST) {
            visible.sort((first, second) -> Integer.compare(second.getRating(), first.getRating()));
        } else if (filter == Filter.LOWEST) {
            visible.sort((first, second) -> Integer.compare(first.getRating(), second.getRating()));
        } else {
            visible.sort((first, second) -> Long.compare(second.getCreatedAt(), first.getCreatedAt()));
        }
        adapter.submitList(visible);
        recycler.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        empty.setText(R.string.review_empty);
        empty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openWriteReview() {
        if (!TokenManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, R.string.review_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!repository.isDeliveredPurchaseEligible(productId)) {
            Toast.makeText(this, R.string.review_delivery_required, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra(WriteReviewActivity.EXTRA_PRODUCT_ID, productId);
        intent.putExtra(WriteReviewActivity.EXTRA_PRODUCT_NAME, productName);
        startActivity(intent);
    }

    private enum Filter {
        ALL,
        IMAGES,
        FIVE_STAR,
        HIGHEST,
        LOWEST
    }
}
