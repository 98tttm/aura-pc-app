package com.example.aura_pc_app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.aura.pc.CheckoutActivity;
import com.aura.pc.ui.blog.BlogActivity;
import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.bumptech.glide.Glide;
import com.example.aura_pc_app.adapter.ProductGalleryPagerAdapter;
import com.example.aura_pc_app.adapter.ProductImageAdapter;
import com.example.aura_pc_app.adapter.RelatedProductEntityAdapter;
import com.example.aura_pc_app.adapter.ReviewAdapter;
import com.example.aura_pc_app.adapter.SpecAdapter;
import com.example.aura_pc_app.adapter.ViewedProductAdapter;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.TokenManager;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.repository.AppRepository;
import com.example.aura_pc_app.data.repository.MockReviewRepository;
import com.example.aura_pc_app.domain.model.Product;
import com.example.aura_pc_app.domain.model.ProductSpec;
import com.example.aura_pc_app.domain.model.Review;
import com.example.aura_pc_app.domain.repository.ReviewRepository;
import com.example.aura_pc_app.domain.repository.mock.MockData;
import com.example.aura_pc_app.utils.LocaleManager;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_PRODUCT_INDEX = "extra_product_index";
    public static final String EXTRA_PRODUCT_NAME = "extra_product_name";
    public static final String EXTRA_PRODUCT_PRICE = "extra_product_price";

    private final Gson gson = new Gson();
    private final NumberFormat currency = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final List<ProductEntity> availableProducts = new ArrayList<>();
    private final List<Review> allReviews = new ArrayList<>();

    private RecyclerView thumbnails;
    private RecyclerView specs;
    private RecyclerView related;
    private RecyclerView viewed;
    private ViewPager2 gallery;
    private TextView productName;
    private TextView ratingText;
    private RatingBar productRatingStars;
    private TextView stockText;
    private TextView reviewCount;
    private TextView currentPrice;
    private TextView oldPrice;
    private TextView discountBadge;
    private TextView description;
    private View specsTitle;
    private View descriptionTitle;
    private View descriptionImageCard;
    private View reviewsContent;
    private TextView reviewOverallRating;
    private RatingBar reviewOverallStars;
    private TextView reviewOverallCount;
    private TextView reviewEligibilityMessage;
    private TextView reviewEmptyMessage;
    private RecyclerView reviewsRecycler;
    private View writeReviewButton;
    private View viewAllReviewsButton;
    private View addToCart;
    private View buyNow;
    private DetailProduct selectedProduct;
    private AppRepository repository;
    private ReviewRepository reviewRepository;
    private ReviewAdapter reviewAdapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        repository = new AppRepository(getApplication(), ApiClient.getInstance(this).getApiService());
        reviewRepository = MockReviewRepository.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        initViews();
        setupReviewSection();
        setupTabs();
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_HOME);
        BottomNavigationHelper.setupHeader(this);
        setupActions();
        loadSelectedProduct();
    }

    private void initViews() {
        gallery = findViewById(R.id.productGalleryPager);
        thumbnails = findViewById(R.id.thumbnailRecyclerView);
        specs = findViewById(R.id.specsRecyclerView);
        related = findViewById(R.id.relatedRecyclerView);
        viewed = findViewById(R.id.viewedRecyclerView);
        productName = findViewById(R.id.productName);
        ratingText = findViewById(R.id.ratingText);
        productRatingStars = findViewById(R.id.productRatingStars);
        stockText = findViewById(R.id.soldCount);
        reviewCount = findViewById(R.id.reviewCount);
        currentPrice = findViewById(R.id.currentPrice);
        oldPrice = findViewById(R.id.oldPrice);
        discountBadge = findViewById(R.id.discountBadge);
        description = findViewById(R.id.productDescription);
        specsTitle = findViewById(R.id.specsTitle);
        descriptionTitle = findViewById(R.id.descriptionTitle);
        descriptionImageCard = findViewById(R.id.descriptionImageCard);
        reviewsContent = findViewById(R.id.reviewsContent);
        reviewOverallRating = findViewById(R.id.reviewOverallRating);
        reviewOverallStars = findViewById(R.id.reviewOverallStars);
        reviewOverallCount = findViewById(R.id.reviewOverallCount);
        reviewEligibilityMessage = findViewById(R.id.reviewEligibilityMessage);
        reviewEmptyMessage = findViewById(R.id.reviewEmptyMessage);
        reviewsRecycler = findViewById(R.id.reviewsRecyclerView);
        writeReviewButton = findViewById(R.id.btnWriteReview);
        viewAllReviewsButton = findViewById(R.id.btnViewAllReviews);
        oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        View actionOverlay = findViewById(R.id.actionOverlay);
        addToCart = actionOverlay == null ? findViewById(R.id.btnAddToCart)
                : actionOverlay.findViewById(R.id.btnAddToCart);
        buyNow = actionOverlay == null ? findViewById(R.id.btnBuyNow)
                : actionOverlay.findViewById(R.id.btnBuyNow);
    }

    private void setupReviewSection() {
        reviewAdapter = new ReviewAdapter();
        reviewsRecycler.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecycler.setAdapter(reviewAdapter);
        reviewsRecycler.setNestedScrollingEnabled(false);

        writeReviewButton.setOnClickListener(v -> openWriteReviewActivity());
        viewAllReviewsButton.setOnClickListener(v -> openReviewList());
        updateReviewEligibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedProduct != null && reviewRepository != null) {
            loadReviews(selectedProduct.id);
        }
    }

    private void setupTabs() {
        TabLayout tabs = findViewById(R.id.productTabs);
        tabs.addTab(tabs.newTab().setText(R.string.tab_specs));
        tabs.addTab(tabs.newTab().setText(R.string.tab_description));
        tabs.addTab(tabs.newTab().setText(R.string.tab_reviews));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        showTab(0);
    }

    private void showTab(int position) {
        boolean showSpecs = position == 0;
        boolean showDescription = position == 1;
        specsTitle.setVisibility(showSpecs ? View.VISIBLE : View.GONE);
        specs.setVisibility(showSpecs ? View.VISIBLE : View.GONE);
        descriptionTitle.setVisibility(showDescription ? View.VISIBLE : View.GONE);
        description.setVisibility(showDescription ? View.VISIBLE : View.GONE);
        descriptionImageCard.setVisibility(showDescription ? View.VISIBLE : View.GONE);
        reviewsContent.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
    }

    private void setupActions() {
        findViewById(R.id.searchIcon).setVisibility(View.GONE);
        findViewById(R.id.shareIcon).setVisibility(View.VISIBLE);
        findViewById(R.id.menuIcon).setOnClickListener(v -> finish());
        findViewById(R.id.shareIcon).setOnClickListener(v -> shareProduct());
        findViewById(R.id.cartContainer).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));
        findViewById(R.id.btnFavorite).setOnClickListener(v ->
                Toast.makeText(this, "Đã cập nhật sản phẩm yêu thích", Toast.LENGTH_SHORT).show());
        View consult = findViewById(R.id.btnConsult);
        if (consult != null) {
            consult.setOnClickListener(v -> startActivity(new Intent(this, BlogActivity.class)));
        }
        addToCart.setOnClickListener(v -> addSelectedProductToCart());
        buyNow.setOnClickListener(v -> openCheckout());
    }

    private void loadSelectedProduct() {
        ApiClient.getInstance(this).getApiService().getProducts().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    bindMockProduct();
                    return;
                }
                Object rawItems = response.body().get("items");
                if (!(rawItems instanceof List) || ((List<?>) rawItems).isEmpty()) {
                    bindMockProduct();
                    return;
                }
                List<Map<String, Object>> items = mapsFromList((List<?>) rawItems);
                if (items.isEmpty()) {
                    bindMockProduct();
                    return;
                }
                availableProducts.clear();
                for (Map<String, Object> item : items) {
                    availableProducts.add(toEntity(item));
                }
                bindProduct(toDetail(selectItem(items)));
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                bindMockProduct();
            }
        });
    }

    private Map<String, Object> selectItem(List<Map<String, Object>> items) {
        String requestedId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (requestedId != null) {
            for (Map<String, Object> item : items) {
                if (requestedId.equals(readString(item, "_id", "id", "productId"))) {
                    return item;
                }
            }
        }
        int requestedIndex = getIntent().getIntExtra(EXTRA_PRODUCT_INDEX, 0);
        return items.get(Math.max(0, Math.min(requestedIndex, items.size() - 1)));
    }

    private void bindProduct(DetailProduct product) {
        selectedProduct = product;
        productName.setText(product.name);
        description.setText(product.description);
        stockText.setText(product.stock > 0
                ? getString(R.string.product_stock_format, product.stock)
                : getString(R.string.product_out_of_stock));

        boolean hasSale = product.salePrice > 0 && product.price > product.salePrice;
        currentPrice.setText(formatPrice(hasSale ? product.salePrice : product.price));
        oldPrice.setVisibility(hasSale ? View.VISIBLE : View.GONE);
        discountBadge.setVisibility(hasSale ? View.VISIBLE : View.GONE);
        if (hasSale) {
            oldPrice.setText(formatPrice(product.price));
            int discount = (int) Math.round((1d - product.salePrice / product.price) * 100d);
            discountBadge.setText("-" + discount + "%");
        }

        setupGallery(product.images);
        specs.setLayoutManager(new GridLayoutManager(this, 2));
        specs.setAdapter(new SpecAdapter(product.specs));
        setupRelatedProducts(product);
        viewed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        viewed.setAdapter(new ViewedProductAdapter(MockData.getViewedProducts()));
        loadReviews(product.id);
    }

    private void loadReviews(String productId) {
        allReviews.clear();
        allReviews.addAll(reviewRepository.getReviews(productId));
        updateReviewSummary();
        updateReviewEligibility();
        renderReviews();
    }

    private void updateReviewSummary() {
        double average = 0;
        for (Review review : allReviews) {
            average += review.getRating();
        }
        if (!allReviews.isEmpty()) {
            average /= allReviews.size();
        }
        String ratingValue = String.format(Locale.US, "%.1f", average);
        reviewOverallRating.setText(getString(R.string.review_rating_out_of_five, ratingValue));
        reviewOverallStars.setRating((float) average);
        productRatingStars.setRating((float) average);
        reviewOverallCount.setText(getString(R.string.review_total_count, allReviews.size()));
        ratingText.setText(ratingValue);
        reviewCount.setText(getString(R.string.reviews_count_format, allReviews.size()));
    }

    private void renderReviews() {
        List<Review> visibleReviews = new ArrayList<>(allReviews);
        visibleReviews.sort((first, second) ->
                Long.compare(second.getCreatedAt(), first.getCreatedAt()));
        if (visibleReviews.size() > 2) {
            visibleReviews = new ArrayList<>(visibleReviews.subList(0, 2));
        }

        reviewAdapter.submitList(visibleReviews);
        reviewsRecycler.setVisibility(visibleReviews.isEmpty() ? View.GONE : View.VISIBLE);
        reviewEmptyMessage.setVisibility(visibleReviews.isEmpty() ? View.VISIBLE : View.GONE);
        reviewEmptyMessage.setText(allReviews.isEmpty()
                ? R.string.review_empty : R.string.review_no_images);
    }

    private void openReviewList() {
        if (selectedProduct == null) {
            return;
        }
        Intent intent = new Intent(this, ReviewListActivity.class);
        intent.putExtra(ReviewListActivity.EXTRA_PRODUCT_ID, selectedProduct.id);
        intent.putExtra(ReviewListActivity.EXTRA_PRODUCT_NAME, selectedProduct.name);
        startActivity(intent);
    }

    private void openWriteReviewActivity() {
        if (!TokenManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, R.string.review_login_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedProduct == null
                || !reviewRepository.isDeliveredPurchaseEligible(selectedProduct.id)) {
            Toast.makeText(this, R.string.review_delivery_required, Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, WriteReviewActivity.class);
        intent.putExtra(WriteReviewActivity.EXTRA_PRODUCT_ID, selectedProduct.id);
        intent.putExtra(WriteReviewActivity.EXTRA_PRODUCT_NAME, selectedProduct.name);
        if (!selectedProduct.images.isEmpty()) {
            intent.putExtra(WriteReviewActivity.EXTRA_PRODUCT_IMAGE, selectedProduct.images.get(0));
        }
        StringBuilder specs = new StringBuilder();
        for (int i = 0; i < Math.min(3, selectedProduct.specs.size()); i++) {
            if (specs.length() > 0) {
                specs.append(" • ");
            }
            specs.append(selectedProduct.specs.get(i).getValue());
        }
        intent.putExtra(WriteReviewActivity.EXTRA_PRODUCT_SPECS, specs.toString());
        startActivity(intent);
    }

    private void updateReviewEligibility() {
        if (writeReviewButton == null || reviewEligibilityMessage == null) {
            return;
        }
        boolean loggedIn = TokenManager.getInstance(this).isLoggedIn();
        boolean eligible = selectedProduct != null
                && reviewRepository.isDeliveredPurchaseEligible(selectedProduct.id);
        // Keep the CTA clickable so the click handler can explain why writing is unavailable.
        writeReviewButton.setEnabled(true);
        if (!loggedIn) {
            reviewEligibilityMessage.setText(R.string.review_login_required);
            reviewEligibilityMessage.setVisibility(View.VISIBLE);
        } else if (!eligible) {
            reviewEligibilityMessage.setText(R.string.review_delivery_required);
            reviewEligibilityMessage.setVisibility(View.VISIBLE);
        } else {
            reviewEligibilityMessage.setVisibility(View.GONE);
        }
    }

    private void setupGallery(List<String> images) {
        List<String> safeImages = images == null || images.isEmpty() ? fallbackImageSources() : images;
        ProductGalleryPagerAdapter pagerAdapter = new ProductGalleryPagerAdapter(
                safeImages, this::showFullscreenImage);
        gallery.setAdapter(pagerAdapter);

        ProductImageAdapter thumbnailAdapter = new ProductImageAdapter(safeImages);
        thumbnails.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thumbnails.setAdapter(thumbnailAdapter);
        thumbnailAdapter.setOnImageClickListener(image -> {
            int index = safeImages.indexOf(image);
            if (index >= 0) {
                gallery.setCurrentItem(index, true);
            }
        });
        gallery.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                thumbnailAdapter.setSelectedPosition(position);
                thumbnails.smoothScrollToPosition(position);
            }
        });
    }

    private void showFullscreenImage(String imageSource) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        PhotoView photoView = new PhotoView(this);
        photoView.setBackgroundColor(Color.BLACK);
        photoView.setContentDescription(getString(R.string.cd_product_gallery_image));
        Glide.with(this).load(imageSource).placeholder(R.drawable.product_case)
                .error(R.drawable.product_case).into(photoView);
        photoView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(photoView);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    private void setupRelatedProducts(DetailProduct product) {
        List<ProductEntity> candidates = new ArrayList<>();
        for (ProductEntity candidate : availableProducts) {
            if (candidate._id.equals(product.id)) {
                continue;
            }
            if (sameCategory(product, candidate)) {
                candidates.add(candidate);
            }
        }
        for (ProductEntity candidate : availableProducts) {
            if (candidates.size() >= 4) {
                break;
            }
            if (!candidate._id.equals(product.id) && !candidates.contains(candidate)) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(createRelatedFallback());
        }
        related.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        related.setAdapter(new RelatedProductEntityAdapter(candidates, this::openRelatedProduct));
    }

    private boolean sameCategory(DetailProduct product, ProductEntity candidate) {
        return product.categoryId != null && !product.categoryId.isEmpty()
                && (product.categoryId.equals(candidate.category_id)
                || candidate.category_ids != null && candidate.category_ids.contains(product.categoryId));
    }

    private void openRelatedProduct(ProductEntity product) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, product._id);
        startActivity(intent);
    }

    private void addSelectedProductToCart() {
        if (selectedProduct == null) {
            return;
        }
        repository.addToCart(selectedProduct.id, 1);
        Toast.makeText(this, R.string.product_added_to_cart, Toast.LENGTH_SHORT).show();
    }

    private void openCheckout() {
        if (selectedProduct == null) {
            return;
        }
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, selectedProduct.id);
        intent.putExtra(EXTRA_PRODUCT_NAME, selectedProduct.name);
        intent.putExtra(EXTRA_PRODUCT_PRICE,
                selectedProduct.salePrice > 0 ? selectedProduct.salePrice : selectedProduct.price);
        startActivity(intent);
    }

    private void shareProduct() {
        if (selectedProduct == null) {
            return;
        }
        double price = selectedProduct.salePrice > 0 ? selectedProduct.salePrice : selectedProduct.price;
        String shortDescription = selectedProduct.description.length() > 140
                ? selectedProduct.description.substring(0, 140) + "..." : selectedProduct.description;
        String text = selectedProduct.name + "\n" + formatPrice(price) + "\n" + shortDescription;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, selectedProduct.name);
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, getString(R.string.product_share_chooser)));
    }

    private DetailProduct toDetail(Map<String, Object> map) {
        DetailProduct detail = new DetailProduct();
        detail.id = valueOrFallback(readString(map, "_id", "id", "productId"), "product-fallback");
        detail.name = valueOrFallback(readString(map, "name", "title"), getString(R.string.product_name_placeholder));
        detail.description = valueOrFallback(readString(map, "description"),
                getString(R.string.product_description_missing));
        detail.price = readDouble(map, "price", "originalPrice");
        detail.salePrice = readDouble(map, "salePrice", "sale_price", "currentPrice");
        double oldPriceValue = readDouble(map, "old_price");
        if (detail.salePrice <= 0 && oldPriceValue > detail.price && detail.price > 0) {
            detail.salePrice = detail.price;
            detail.price = oldPriceValue;
        } else if (detail.price <= 0) {
            detail.price = oldPriceValue;
        }
        detail.stock = readInt(map, "stock", "quantity");
        detail.categoryId = readString(map, "category_id", "categoryId", "category");
        detail.images = parseImages(map.get("images"));
        detail.specs = parseSpecs(map.get("specs"));
        return detail;
    }

    private ProductEntity toEntity(Map<String, Object> map) {
        ProductEntity entity = new ProductEntity();
        entity._id = valueOrFallback(readString(map, "_id", "id", "productId"), "product-" + availableProducts.size());
        entity.name = valueOrFallback(readString(map, "name", "title"), getString(R.string.product_name_placeholder));
        entity.price = readDouble(map, "price", "originalPrice");
        double sale = readDouble(map, "salePrice", "sale_price", "currentPrice");
        double oldPriceValue = readDouble(map, "old_price");
        if (sale <= 0 && oldPriceValue > entity.price && entity.price > 0) {
            sale = entity.price;
            entity.price = oldPriceValue;
        } else if (entity.price <= 0) {
            entity.price = oldPriceValue;
        }
        entity.salePrice = sale > 0 ? sale : null;
        entity.stock = readInt(map, "stock", "quantity");
        entity.category_id = readString(map, "category_id", "categoryId", "category");
        entity.category_ids = parseStringList(map.get("category_ids"));
        entity.images = gson.toJson(parseImages(map.get("images")));
        entity.specs = gson.toJson(map.get("specs"));
        entity.active = true;
        return entity;
    }

    private List<ProductSpec> parseSpecs(Object rawSpecs) {
        List<ProductSpec> result = new ArrayList<>();
        try {
            JsonObject object;
            if (rawSpecs instanceof Map) {
                object = gson.toJsonTree(rawSpecs).getAsJsonObject();
            } else if (rawSpecs instanceof String && !((String) rawSpecs).trim().isEmpty()) {
                object = JsonParser.parseString((String) rawSpecs).getAsJsonObject();
            } else {
                object = null;
            }
            if (object != null) {
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        result.add(new ProductSpec(entry.getKey(), entry.getValue().getAsString(),
                                specIcon(entry.getKey())));
                    }
                }
            }
        } catch (RuntimeException ignored) {
            result.clear();
        }
        return result.isEmpty() ? MockData.getDetailProduct().getSpecs() : result;
    }

    private int specIcon(String label) {
        String lower = label == null ? "" : label.toLowerCase(Locale.ROOT);
        if (lower.contains("gpu") || lower.contains("card") || lower.contains("vga")) {
            return R.drawable.ic_gpu;
        }
        if (lower.contains("ram") || lower.contains("memory")) {
            return R.drawable.ic_ram;
        }
        if (lower.contains("ssd") || lower.contains("storage") || lower.contains("disk")) {
            return R.drawable.ic_ssd;
        }
        return R.drawable.ic_cpu;
    }

    private List<String> parseImages(Object rawImages) {
        if (rawImages instanceof List) {
            List<String> images = new ArrayList<>();
            for (Object image : (List<?>) rawImages) {
                if (image != null && !String.valueOf(image).trim().isEmpty()) {
                    images.add(String.valueOf(image));
                }
            }
            return images;
        }
        if (rawImages instanceof String && !((String) rawImages).trim().isEmpty()) {
            try {
                JsonElement parsed = JsonParser.parseString((String) rawImages);
                if (parsed.isJsonArray()) {
                    List<String> images = new ArrayList<>();
                    for (JsonElement element : parsed.getAsJsonArray()) {
                        images.add(element.getAsString());
                    }
                    return images;
                }
                return Collections.singletonList((String) rawImages);
            } catch (RuntimeException ignored) {
                return Collections.singletonList((String) rawImages);
            }
        }
        return new ArrayList<>();
    }

    private List<String> parseStringList(Object raw) {
        return raw instanceof List ? parseImages(raw) : new ArrayList<>();
    }

    private List<Map<String, Object>> mapsFromList(List<?> rawItems) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object raw : rawItems) {
            if (raw instanceof Map) {
                items.add((Map<String, Object>) raw);
            }
        }
        return items;
    }

    private void bindMockProduct() {
        Product mock = MockData.getDetailProduct();
        DetailProduct detail = new DetailProduct();
        detail.id = valueOrFallback(getIntent().getStringExtra(EXTRA_PRODUCT_ID), "mock-aura-nova-x9");
        detail.name = mock.getName();
        detail.description = mock.getDescription();
        detail.price = parseFormattedPrice(mock.getOldPrice());
        detail.salePrice = parseFormattedPrice(mock.getCurrentPrice());
        detail.stock = 15;
        detail.specs = mock.getSpecs();
        detail.images = fallbackImageSources();
        availableProducts.clear();
        availableProducts.addAll(createRelatedFallback());
        bindProduct(detail);
    }

    private List<ProductEntity> createRelatedFallback() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(fallbackEntity("mock-monitor", "Màn hình Aura 4K", 12500000, R.drawable.pc_main_1));
        products.add(fallbackEntity("mock-keyboard", "Bàn phím cơ Aura", 2450000, R.drawable.pc_main_2));
        products.add(fallbackEntity("mock-headset", "Tai nghe Aura Pro", 3800000, R.drawable.pc_main_3));
        return products;
    }

    private ProductEntity fallbackEntity(String id, String name, double price, int imageRes) {
        ProductEntity entity = new ProductEntity();
        entity._id = id;
        entity.name = name;
        entity.price = price;
        entity.stock = 10;
        entity.images = gson.toJson(Collections.singletonList(resourceUri(imageRes)));
        return entity;
    }

    private List<String> fallbackImageSources() {
        List<String> images = new ArrayList<>();
        images.add(resourceUri(R.drawable.pc_main_1));
        images.add(resourceUri(R.drawable.pc_main_2));
        images.add(resourceUri(R.drawable.pc_main_3));
        return images;
    }

    private String resourceUri(int resourceId) {
        return "android.resource://" + getPackageName() + "/" + resourceId;
    }

    private String readString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private double readDouble(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    private int readInt(Map<String, Object> map, String... keys) {
        return (int) Math.max(0, readDouble(map, keys));
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String formatPrice(double price) {
        return price > 0 ? currency.format(price) + "đ" : "Liên hệ";
    }

    private double parseFormattedPrice(String price) {
        if (price == null) {
            return 0;
        }
        try {
            return Double.parseDouble(price.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static class DetailProduct {
        String id;
        String name;
        String description;
        String categoryId;
        double price;
        double salePrice;
        int stock;
        List<String> images = new ArrayList<>();
        List<ProductSpec> specs = new ArrayList<>();
    }

}
