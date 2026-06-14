package com.example.aura_pc_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.adapter.ReviewImageAdapter;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.data.repository.MockReviewRepository;
import com.example.aura_pc_app.domain.model.Review;
import com.example.aura_pc_app.domain.repository.ReviewRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class WriteReviewActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "write_review_product_id";
    public static final String EXTRA_PRODUCT_NAME = "write_review_product_name";
    public static final String EXTRA_PRODUCT_IMAGE = "write_review_product_image";
    public static final String EXTRA_PRODUCT_SPECS = "write_review_product_specs";

    private final List<String> selectedImages = new ArrayList<>();
    private ReviewRepository repository;
    private ReviewImageAdapter imageAdapter;
    private RecyclerView imagesRecycler;
    private String productId;
    private int selectedRating;
    private TextView[] ratingStars;
    private TextView ratingLabel;

    private final ActivityResultLauncher<String> imagePicker = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                int remainingSlots = 3 - selectedImages.size();
                if (uris != null) {
                    for (int i = 0; i < Math.min(remainingSlots, uris.size()); i++) {
                        selectedImages.add(uris.get(i).toString());
                    }
                    if (uris.size() > remainingSlots) {
                        Toast.makeText(this, R.string.review_image_limit, Toast.LENGTH_SHORT).show();
                    }
                }
                imageAdapter.setImages(selectedImages);
                imagesRecycler.setVisibility(selectedImages.isEmpty() ? View.GONE : View.VISIBLE);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);
        repository = MockReviewRepository.getInstance();
        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        findViewById(R.id.writeReviewBack).setOnClickListener(v -> finish());
        bindProductSummary();
        setupRating();
        setupContentCounter();
        setupImages();
        findViewById(R.id.btnChooseReviewImages).setOnClickListener(v -> {
            if (selectedImages.size() >= 3) {
                Toast.makeText(this, R.string.review_image_limit, Toast.LENGTH_SHORT).show();
                return;
            }
            imagePicker.launch("image/*");
        });
        findViewById(R.id.btnSubmitReview).setOnClickListener(v -> submit());
    }

    private void bindProductSummary() {
        TextView title = findViewById(R.id.writeReviewTitle);
        TextView name = findViewById(R.id.writeReviewProductName);
        TextView specs = findViewById(R.id.writeReviewProductSpecs);
        ImageView image = findViewById(R.id.writeReviewProductImage);

        title.setText(R.string.review_write_header);
        String productName = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        name.setText(productName == null || productName.trim().isEmpty()
                ? getString(R.string.product_name_placeholder) : productName);
        String specValue = getIntent().getStringExtra(EXTRA_PRODUCT_SPECS);
        specs.setText(specValue == null || specValue.trim().isEmpty()
                ? getString(R.string.review_mock_variant) : specValue);
        Glide.with(this)
                .load(getIntent().getStringExtra(EXTRA_PRODUCT_IMAGE))
                .placeholder(R.drawable.product_case)
                .error(R.drawable.product_case)
                .into(image);
    }

    private void setupRating() {
        ratingStars = new TextView[]{
                findViewById(R.id.writeReviewStar1),
                findViewById(R.id.writeReviewStar2),
                findViewById(R.id.writeReviewStar3),
                findViewById(R.id.writeReviewStar4),
                findViewById(R.id.writeReviewStar5)
        };
        ratingLabel = findViewById(R.id.writeReviewRatingLabel);
        for (int i = 0; i < ratingStars.length; i++) {
            int rating = i + 1;
            ratingStars[i].setContentDescription(getString(R.string.review_star_description, rating));
            ratingStars[i].setOnClickListener(v -> selectRating(rating));
        }
    }

    private void selectRating(int rating) {
        selectedRating = rating;
        for (int i = 0; i < ratingStars.length; i++) {
            ratingStars[i].setTextColor(getColor(
                    i < rating ? R.color.review_star_selected : R.color.review_star_unselected));
        }
        String[] labels = getResources().getStringArray(R.array.review_rating_labels);
        ratingLabel.setText(labels[rating - 1]);
    }

    private void setupContentCounter() {
        EditText content = findViewById(R.id.writeReviewContent);
        TextView counter = findViewById(R.id.writeReviewCounter);
        content.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                counter.setText(getString(R.string.review_content_counter, value.length()));
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
    }

    private void setupImages() {
        imagesRecycler = findViewById(R.id.writeReviewImagesRecycler);
        imagesRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new ReviewImageAdapter(selectedImages);
        imagesRecycler.setAdapter(imageAdapter);
    }

    private void submit() {
        if (!TokenManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, R.string.review_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!repository.isDeliveredPurchaseEligible(productId)) {
            Toast.makeText(this, R.string.review_delivery_required, Toast.LENGTH_LONG).show();
            return;
        }

        EditText content = findViewById(R.id.writeReviewContent);
        String selectedContent = content.getText().toString().trim();
        if (selectedRating < 1) {
            Toast.makeText(this, R.string.review_rating_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedContent.isEmpty()) {
            content.setError(getString(R.string.review_content_required));
            return;
        }

        long now = System.currentTimeMillis();
        Review review = new Review(
                "local-review-" + now,
                productId,
                currentUserName(),
                selectedRating,
                selectedContent,
                new ArrayList<>(selectedImages),
                now,
                true,
                null
        );
        repository.addReview(review);
        Toast.makeText(this, R.string.review_submitted, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private String currentUserName() {
        String userJson = TokenManager.getInstance(this).getCurrentUserJson();
        if (userJson != null) {
            try {
                JsonObject user = JsonParser.parseString(userJson).getAsJsonObject();
                for (String key : new String[]{"fullName", "name", "username"}) {
                    if (user.has(key) && !user.get(key).isJsonNull()) {
                        String value = user.get(key).getAsString().trim();
                        if (!value.isEmpty()) {
                            return value;
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // Fall back to the neutral mock user name.
            }
        }
        return getString(R.string.review_default_user);
    }
}
