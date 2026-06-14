package com.example.aura_pc_app.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.ui.categories.CategoriesActivity;
import com.aura.pc.ui.products.AuraProductsActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.aura.pc.ui.productdetail.ProductDetailActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.adapter.HomeProductAdapter;
import com.example.aura_pc_app.adapter.HomeSaleProductAdapter;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.db.entity.SearchHistoryEntity;
import com.example.aura_pc_app.databinding.ActivityHomeBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;

public class HomeActivity extends BaseActivity<ActivityHomeBinding> {
    private static final int HISTORY_LIMIT = 5;
    private static final long SEARCH_DEBOUNCE_MS = 300L;
    private static final String[] POPULAR_QUERIES = {
            "Laptop gaming",
            "RTX 4060",
            "PC gaming",
            "Man hinh 27",
            "Logitech",
            "SSD NVMe"
    };

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService historyExecutor = Executors.newSingleThreadExecutor();
    private final List<ProductEntity> allProducts = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();

    private HomeProductAdapter productAdapter;
    private HomeSaleProductAdapter saleProductAdapter;
    private boolean collapsedHeaderVisible;
    private String selectedProductCategoryId = "laptop";
    private int selectedSaleCampaignIndex;
    private int selectedSaleDateIndex;
    private final Map<String, List<CategoryChip>> childCategoriesByParent = new HashMap<>();
    private final List<ProductEntity> allSaleProducts = new ArrayList<>();
    private final Handler saleCountdownHandler = new Handler(Looper.getMainLooper());
    private final Runnable saleCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            updateSaleCountdown();
            saleCountdownHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_HOME);
        setupSaleSection();
        setupProductList();
        setupHomeSearch();
        setupHomeActions();
        setupHomeProductTabs();
        loadHomeCategories();
        loadSaleProducts();
        loadHomeProducts(selectedProductCategoryId);
    }

    @Override
    protected ActivityHomeBinding inflateBinding() {
        return ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onDestroy() {
        saleCountdownHandler.removeCallbacks(saleCountdownRunnable);
        super.onDestroy();
    }

    private void setupHomeActions() {
        binding.topCartButton.setOnClickListener(v -> openCart());
        binding.topNotificationsButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        binding.topMenuButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_menu_pending, Toast.LENGTH_SHORT).show());
        binding.searchFilterButton.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));
        binding.contextualFab.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_chat_pending, Toast.LENGTH_SHORT).show());
        binding.homeHeroCard.setOnClickListener(v -> openProductDetail());
        binding.homeHeroCta.setOnClickListener(v -> openProductDetail());
        binding.homeQuickCategories.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));
        binding.homeViewAllProducts.setOnClickListener(v -> openProductDetail());
        setupCategoryStrip();
        setupComponentStrip();
        setupCollapsedHeader();
    }

    private void setupCollapsedHeader() {
        binding.stickyTopMenuButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_menu_pending, Toast.LENGTH_SHORT).show());
        binding.stickyNotificationsButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        binding.stickyCartButton.setOnClickListener(v -> openCart());
        binding.stickySearchFilterButton.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));
        binding.homeScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> updateCollapsedHeader(scrollY));
        updateCollapsedHeader(binding.homeScrollView.getScrollY());
    }

    private void updateCollapsedHeader(int scrollY) {
        boolean shouldShow = scrollY >= dpToPx(82);
        if (shouldShow == collapsedHeaderVisible) {
            return;
        }
        collapsedHeaderVisible = shouldShow;
        binding.stickyCollapsedHeader.animate().cancel();
        if (shouldShow) {
            binding.stickyCollapsedHeader.setVisibility(View.VISIBLE);
            binding.stickyCollapsedHeader.setTranslationY(-dpToPx(8));
            binding.stickyCollapsedHeader.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(140)
                    .start();
        } else {
            binding.stickyCollapsedHeader.animate()
                    .alpha(0f)
                    .translationY(-dpToPx(8))
                    .setDuration(120)
                    .withEndAction(() -> binding.stickyCollapsedHeader.setVisibility(View.GONE))
                    .start();
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setupProductList() {
        productAdapter = new HomeProductAdapter(new HomeProductAdapter.ProductClickListener() {
            @Override
            public void onProductClick(ProductEntity product) {
                openProductDetail(product);
            }

            @Override
            public void onCartClick(ProductEntity product) {
                addProductToCart(product);
            }
        });
        binding.productRecyclerView.setLayoutManager(new GridLayoutManager(this, 2, GridLayoutManager.HORIZONTAL, false));
        binding.productRecyclerView.setAdapter(productAdapter);
        binding.productRecyclerView.setHasFixedSize(false);
        binding.productRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                updateRecyclerIndicator(
                        binding.productRecyclerView,
                        binding.homeProductIndicatorTrack,
                        binding.homeProductIndicatorThumb);
            }
        });
        binding.productRecyclerView.post(() -> updateRecyclerIndicator(
                binding.productRecyclerView,
                binding.homeProductIndicatorTrack,
                binding.homeProductIndicatorThumb));
    }

    private void observeProducts() {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        viewModel.getProducts().observe(this, products -> {
            productAdapter.setProducts(products);
            binding.loadingProgress.setVisibility(View.GONE);
        });
        viewModel.getProductAdded().observe(this, added -> {
            if (Boolean.TRUE.equals(added)) {
                Toast.makeText(this, R.string.cart_added, Toast.LENGTH_SHORT).show();
                viewModel.clearProductAdded();
            }
        });
        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void consumeHomeCategories(List<Map<String, Object>> rawCategories) {
        childCategoriesByParent.clear();
        for (Map<String, Object> raw : rawCategories) {
            String parentId = stringValue(raw.get("parent_id"));
            if (parentId.isEmpty()) {
                continue;
            }
            String id = stringValue(raw.get("category_id"));
            String name = stringValue(raw.get("name"));
            if (id.isEmpty() || name.isEmpty()) {
                continue;
            }
            List<CategoryChip> children = childCategoriesByParent.get(parentId);
            if (children == null) {
                children = new ArrayList<>();
                childCategoriesByParent.put(parentId, children);
            }
            children.add(new CategoryChip(id, name));
        }
    }

    private void renderHomeBrandChips() {
        binding.homeBrandChipRow.removeAllViews();
        List<CategoryChip> chips = childCategoriesByParent.get(selectedProductCategoryId);
        if (chips == null || chips.isEmpty()) {
            chips = defaultHomeBrandChips(selectedProductCategoryId);
        }
        int count = Math.min(8, chips.size());
        for (int i = 0; i < count; i++) {
            CategoryChip chip = chips.get(i);
            View chipView = createHomeBrandChip(chip);
            binding.homeBrandChipRow.addView(chipView);
        }
    }

    private List<CategoryChip> defaultHomeBrandChips(String categoryId) {
        List<CategoryChip> chips = new ArrayList<>();
        if ("laptop".equals(categoryId)) {
            chips.add(new CategoryChip("laptop-asus", "ASUS"));
            chips.add(new CategoryChip("laptop-msi", "MSI"));
            chips.add(new CategoryChip("laptop-lenovo", "LENOVO"));
            chips.add(new CategoryChip("laptop-acer", "ACER"));
        } else if ("pc".equals(categoryId)) {
            chips.add(new CategoryChip("pc-asus", "PC Asus"));
            chips.add(new CategoryChip("pc-msi", "PC MSI"));
            chips.add(new CategoryChip("pc-gigabyte", "PC Gigabyte"));
            chips.add(new CategoryChip("pc-gaming", "PC gaming"));
        } else if ("man-hinh".equals(categoryId)) {
            chips.add(new CategoryChip("man-hinh-asus", "Asus"));
            chips.add(new CategoryChip("man-hinh-dell", "Dell"));
            chips.add(new CategoryChip("man-hinh-msi", "MSI"));
            chips.add(new CategoryChip("man-hinh-samsung", "Samsung"));
        } else {
            chips.add(new CategoryChip("balo-tui-xach", "Balo"));
            chips.add(new CategoryChip("phu-kien-pc", "Phụ kiện PC"));
            chips.add(new CategoryChip("thiet-bi-mang", "Thiết bị mạng"));
        }
        return chips;
    }

    private View createHomeBrandChip(CategoryChip chip) {
        int logoRes = logoForCategoryChip(chip.name);
        if (logoRes != 0) {
            ImageView logo = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(66), dpToPx(32));
            params.setMarginEnd(dpToPx(10));
            logo.setLayoutParams(params);
            logo.setBackgroundResource(R.drawable.bg_home_brand_chip);
            logo.setContentDescription(chip.name);
            logo.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            logo.setImageResource(logoRes);
            logo.setOnClickListener(v -> openCategoryProducts(chip.categoryId, chip.name));
            return logo;
        }

        TextView text = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(32));
        params.setMarginEnd(dpToPx(10));
        text.setLayoutParams(params);
        text.setBackgroundResource(R.drawable.bg_home_brand_chip);
        text.setGravity(Gravity.CENTER);
        text.setIncludeFontPadding(false);
        text.setMinWidth(dpToPx(66));
        text.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        text.setSingleLine(true);
        text.setText(chip.name);
        text.setTextColor(getColor(R.color.aura_ink));
        text.setTextSize(10);
        text.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        text.setOnClickListener(v -> openCategoryProducts(chip.categoryId, chip.name));
        return text;
    }

    private int logoForCategoryChip(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.contains("asus")) return R.drawable.figma_brand_asus;
        if (normalized.contains("dell")) return R.drawable.figma_brand_dell;
        if (normalized.contains("msi")) return R.drawable.figma_brand_msi;
        if (normalized.contains("lenovo")) return R.drawable.figma_brand_lenovo;
        if (normalized.contains("acer") || normalized.contains("predator")) return R.drawable.figma_brand_acer;
        return 0;
    }

    private void loadSaleProducts() {
        ApiClient.getInstance(this).getApiService()
                .getProductsByCategory(null, 1, 150)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        allSaleProducts.clear();
                        if (response.isSuccessful() && response.body() != null) {
                            List<ProductEntity> products = mapHomeProducts(readItems(response.body()));
                            for (ProductEntity product : products) {
                                if (isSaleProduct(product)) {
                                    allSaleProducts.add(product);
                                }
                            }
                            allSaleProducts.sort(Comparator.comparingDouble(HomeActivity::discountRatio).reversed());
                        }
                        if (allSaleProducts.isEmpty()) {
                            allSaleProducts.addAll(createSaleFallbackProducts());
                        }
                        renderSaleProducts();
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        allSaleProducts.clear();
                        allSaleProducts.addAll(createSaleFallbackProducts());
                        renderSaleProducts();
                    }
                });
    }

    private void renderSaleProducts() {
        if (saleProductAdapter == null) {
            return;
        }
        List<ProductEntity> source = allSaleProducts.isEmpty() ? createSaleFallbackProducts() : allSaleProducts;
        List<ProductEntity> selected = new ArrayList<>();
        int offset = selectedSaleDateIndex * 30 + selectedSaleCampaignIndex * 10;
        for (int i = offset; selected.size() < 10 && i < offset + source.size(); i++) {
            ProductEntity product = source.get(i % source.size());
            if (!containsProduct(selected, product)) {
                selected.add(product);
            }
        }
        saleProductAdapter.setProducts(selected, selectedSaleCampaignIndex, selectedSaleDateIndex);
        binding.homeSaleRecyclerView.scrollToPosition(0);
        binding.homeSaleRecyclerView.post(() -> updateRecyclerIndicator(
                binding.homeSaleRecyclerView,
                binding.homeSaleIndicatorTrack,
                binding.homeSaleIndicatorThumb));
    }

    private boolean containsProduct(List<ProductEntity> products, ProductEntity candidate) {
        String candidateId = candidate._id == null ? "" : candidate._id;
        for (ProductEntity product : products) {
            if (candidateId.equals(product._id)) {
                return true;
            }
        }
        return false;
    }

    private static double discountRatio(ProductEntity product) {
        double currentPrice = product.salePrice != null && product.salePrice > 0 ? product.salePrice : product.price;
        double oldPrice = product.oldPrice != null && product.oldPrice > currentPrice
                ? product.oldPrice
                : (product.salePrice != null && product.salePrice > 0 && product.price > product.salePrice ? product.price : 0);
        if (oldPrice <= currentPrice || oldPrice <= 0) {
            return 0;
        }
        return (oldPrice - currentPrice) / oldPrice;
    }

    private boolean isSaleProduct(ProductEntity product) {
        return discountRatio(product) > 0;
    }

    private void updateRecyclerIndicator(RecyclerView recyclerView, View track, View thumb) {
        int range = recyclerView.computeHorizontalScrollRange();
        int extent = recyclerView.computeHorizontalScrollExtent();
        int offset = recyclerView.computeHorizontalScrollOffset();
        int scrollRange = Math.max(0, range - extent);
        int trackWidth = track.getWidth();
        int thumbWidth = Math.max(dpToPx(18), thumb.getWidth());

        if (scrollRange == 0 || trackWidth == 0 || thumbWidth == 0) {
            thumb.setTranslationX(0f);
            return;
        }

        float progress = offset / (float) scrollRange;
        float maxOffset = Math.max(0, trackWidth - thumbWidth);
        thumb.setTranslationX(progress * maxOffset);
    }

    private void loadHomeProducts(String categoryId) {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        ApiClient.getInstance(this).getApiService()
                .getProductsByCategory(categoryId, 1, 20)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        binding.loadingProgress.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            List<ProductEntity> products = mapHomeProducts(readItems(response.body()));
                            productAdapter.setProducts(products.isEmpty() ? createFallbackProducts() : products);
                            return;
                        }
                        productAdapter.setProducts(createFallbackProducts());
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        binding.loadingProgress.setVisibility(View.GONE);
                        productAdapter.setProducts(createFallbackProducts());
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readItems(Map<String, Object> body) {
        Object items = body.get("items");
        if (items instanceof List) {
            return (List<Map<String, Object>>) items;
        }
        return new ArrayList<>();
    }

    private List<ProductEntity> mapHomeProducts(List<Map<String, Object>> rawProducts) {
        List<ProductEntity> products = new ArrayList<>();
        int count = rawProducts.size();
        for (int i = 0; i < count; i++) {
            Map<String, Object> raw = rawProducts.get(i);
            ProductEntity product = new ProductEntity();
            product._id = stringValue(raw.get("_id"));
            if (product._id.isEmpty()) {
                product._id = stringValue(raw.get("product_id"));
            }
            product.name = stringValue(raw.get("name"));
            product.slug = stringValue(raw.get("slug"));
            product.price = numberValue(raw.get("price"));
            product.oldPrice = nullableNumber(raw.get("old_price"));
            product.category_id = stringValue(raw.get("category_id"));
            product.brand = stringValue(raw.get("brand"));
            product.imageUrl = firstImageUrl(raw.get("images"));
            product.active = true;
            products.add(product);
        }
        return products;
    }

    @SuppressWarnings("unchecked")
    private String firstImageUrl(Object value) {
        if (value instanceof List && !((List<?>) value).isEmpty()) {
            return String.valueOf(((List<?>) value).get(0));
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.startsWith("[") && text.contains("http")) {
                int start = text.indexOf("http");
                int end = text.indexOf('"', start);
                return end > start ? text.substring(start, end) : text.substring(start);
            }
            return text;
        }
        return "";
    }

    private String stringValue(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? "" : text.trim();
    }

    private double numberValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0;
    }

    private Double nullableNumber(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return null;
    }

    private void scheduleDebouncedSearch(String rawQuery) {
        isSearchActivated = true;
        String nextQuery = sanitizeKeyword(rawQuery);
        if (!nextQuery.equals(committedSuggestionQuery)) {
            hideSuggestionsForCommittedQuery = false;
            committedSuggestionQuery = "";
        }
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        pendingSearchRunnable = () -> applyHomeSearch(rawQuery);
        searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
    }

    private void selectSuggestion(String keyword) {
        String selectedQuery = sanitizeKeyword(keyword);
        if (selectedQuery.isEmpty()) {
            return;
        }

        isSearchActivated = true;
        hideSuggestionsForCommittedQuery = true;
        committedSuggestionQuery = selectedQuery;
        binding.homeProductSearchView.setQuery(selectedQuery, false);
        runSearchImmediately(selectedQuery, true);
        binding.homeProductSearchView.clearFocus();
        hideSuggestionPanel();
    }

    private void runSearchImmediately(String rawQuery, boolean persist) {
        isSearchActivated = true;
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        applyHomeSearch(rawQuery);
        if (persist) {
            saveSearchQuery(rawQuery);
        }
    }

    private void applyHomeSearch(String rawQuery) {
        activeHomeQuery = sanitizeKeyword(rawQuery);
        updateSuggestions(activeHomeQuery);

        if (activeHomeQuery.isEmpty()) {
            binding.homeSearchResultsSection.setVisibility(View.GONE);
            binding.homeSearchEmptyText.setVisibility(View.GONE);
            searchResultAdapter.setProducts(new ArrayList<>(), "");
            return;
        }

        List<ProductEntity> filtered = filterProducts(activeHomeQuery);
        binding.homeSearchResultsSection.setVisibility(View.VISIBLE);
        binding.homeSearchResultsRecyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        binding.homeSearchEmptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        searchResultAdapter.setProducts(filtered, activeHomeQuery);
    }

    private List<ProductEntity> filterProducts(String query) {
        List<ProductEntity> filtered = new ArrayList<>();
        for (ProductEntity product : allProducts) {
            if (query.isEmpty() || matchesProduct(product, query)) {
                filtered.add(product);
            }
        }
        return filtered;
    }

    private boolean matchesProduct(ProductEntity product, String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        return contains(product.name, lowerQuery)
                || contains(product.brand, lowerQuery)
                || contains(product.slug, lowerQuery)
                || contains(product.specs, lowerQuery)
                || contains(product.category_id, lowerQuery)
                || containsCategoryIds(product.category_ids, lowerQuery);
    }

    private boolean containsCategoryIds(List<String> categoryIds, String lowerQuery) {
        if (categoryIds == null) {
            return false;
        }
        for (String categoryId : categoryIds) {
            if (contains(categoryId, lowerQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    private void updateSuggestions(String query) {
        if (!isSearchActivated) {
            hideSuggestionPanel();
            return;
        }

        if (hideSuggestionsForCommittedQuery && query.equals(committedSuggestionQuery)) {
            hideSuggestionPanel();
            return;
        }

        List<String> suggestions;
        if (query.isEmpty()) {
            binding.homeSearchSuggestionTitle.setText(R.string.search_popular_title);
            suggestions = buildIdleSuggestions();
        } else {
            binding.homeSearchSuggestionTitle.setText(R.string.search_suggestion_title);
            suggestions = buildKeywordSuggestions(query);
        }
        binding.homeSearchSuggestionPanel.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
        suggestionAdapter.submitList(suggestions, query);
    }

    private void hideSuggestionPanel() {
        binding.homeSearchSuggestionPanel.setVisibility(View.GONE);
        suggestionAdapter.submitList(new ArrayList<>(), activeHomeQuery);
    }

    private void activateHomeSearch() {
        isSearchActivated = true;
        updateSuggestions(activeHomeQuery);
    }

    private List<String> buildIdleSuggestions() {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(recentSearches);
        for (String query : POPULAR_QUERIES) {
            merged.add(query);
        }
        return limitSuggestions(merged, 8);
    }

    private List<String> buildKeywordSuggestions(String query) {
        Set<String> suggestions = new LinkedHashSet<>();
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        for (ProductEntity product : allProducts) {
            if (matchesProduct(product, query)) {
                String name = product.name == null ? "" : product.name.trim();
                if (!name.isEmpty()) {
                    suggestions.add(name);
                }
            }
            if (suggestions.size() >= 6) {
                break;
            }
        }

        for (String popular : POPULAR_QUERIES) {
            if (popular.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                suggestions.add(popular);
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add(query);
        }
        return limitSuggestions(suggestions, 6);
    }

    private List<String> limitSuggestions(Set<String> values, int limit) {
        List<String> limited = new ArrayList<>();
        for (String value : values) {
            if (limited.size() == limit) {
                break;
            }
            limited.add(value);
        }
        return limited;
    }

    private void loadSearchHistory() {
        historyExecutor.execute(() -> {
            List<SearchHistoryEntity> entities = searchHistoryDao.getRecent(HISTORY_LIMIT);
            List<String> keywords = toKeywords(entities);
            runOnUiThread(() -> {
                recentSearches.clear();
                recentSearches.addAll(keywords);
                updateSuggestions(activeHomeQuery);
            });
        });
    }

    private void saveSearchQuery(String rawQuery) {
        String keyword = sanitizeKeyword(rawQuery);
        if (keyword.isEmpty()) {
            return;
        }
        historyExecutor.execute(() -> {
            searchHistoryDao.upsert(new SearchHistoryEntity(
                    normalizeKeyword(keyword),
                    keyword,
                    System.currentTimeMillis()
            ));
            searchHistoryDao.pruneToLimit(HISTORY_LIMIT);
            List<String> keywords = toKeywords(searchHistoryDao.getRecent(HISTORY_LIMIT));
            runOnUiThread(() -> {
                recentSearches.clear();
                recentSearches.addAll(keywords);
                updateSuggestions(activeHomeQuery);
            });
        });
    }

    private List<String> toKeywords(List<SearchHistoryEntity> entities) {
        List<String> keywords = new ArrayList<>();
        if (entities == null) {
            return keywords;
        }
        for (SearchHistoryEntity entity : entities) {
            if (entity.keyword != null && !entity.keyword.trim().isEmpty()) {
                keywords.add(entity.keyword.trim());
            }
        }
        return keywords;
    }

    private String sanitizeKeyword(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        return rawQuery.trim().replaceAll("\\s+", " ");
    }

    private String normalizeKeyword(String keyword) {
        return sanitizeKeyword(keyword).toLowerCase(Locale.ROOT);
    }

    private void setupCategoryStrip() {
        String[] titles = {
                "Laptop", "PC", "Màn hình", "Linh kiện", "Khác", "Xem tất cả"
        };
        int[] icons = {
                R.drawable.figma_cat_laptop,
                R.drawable.figma_cat_gaming_pc,
                R.drawable.figma_cat_monitor_figma,
                R.drawable.figma_cat_keyboard_figma,
                R.drawable.figma_cat_component,
                R.drawable.figma_cat_component_ryzen,
                R.drawable.figma_cat_accessory
        };

        bindCategoryItems(binding.homeCategoryStrip, titles, icons);
        setupCategoryScrollIndicator();
    }

    private void setupCategoryScrollIndicator() {
        binding.homeCategoryScroll.getViewTreeObserver().addOnScrollChangedListener(this::updateCategoryScrollIndicator);
        binding.homeCategoryScroll.post(this::updateCategoryScrollIndicator);
    }

    private void updateCategoryScrollIndicator() {
        int contentWidth = binding.homeCategoryStrip.getWidth();
        int viewportWidth = binding.homeCategoryScroll.getWidth();
        int scrollRange = Math.max(0, contentWidth - viewportWidth);
        int trackWidth = binding.homeCategoryIndicatorTrack.getWidth();
        int thumbWidth = Math.max(dpToPx(18), binding.homeCategoryIndicatorThumb.getWidth());

        if (scrollRange == 0 || trackWidth == 0 || thumbWidth == 0) {
            binding.homeCategoryIndicatorThumb.setTranslationX(0f);
            return;
        }

        float progress = binding.homeCategoryScroll.getScrollX() / (float) scrollRange;
        float maxOffset = Math.max(0, trackWidth - thumbWidth);
        binding.homeCategoryIndicatorThumb.setTranslationX(progress * maxOffset);
    }

    private void setupComponentStrip() {
        int[] titles = {
                R.string.category_cpu,
                R.string.home_component_case,
                R.string.home_component_hdd,
                R.string.category_mainboard,
                R.string.home_component_psu,
                R.string.home_component_fan,
                R.string.category_ram,
                R.string.category_storage,
                R.string.home_component_cooler,
                R.string.category_gpu
        };
        int[] icons = {
                R.drawable.figma_component_cpu,
                R.drawable.figma_component_case,
                R.drawable.figma_component_hdd,
                R.drawable.figma_component_mainboard,
                R.drawable.figma_component_psu,
                R.drawable.figma_component_fan,
                R.drawable.figma_component_ram,
                R.drawable.figma_component_ssd,
                R.drawable.figma_component_cooler,
                R.drawable.figma_component_vga
        };

        bindCategoryItems(binding.homeComponentStrip, titles, icons);
    }

    private void bindCategoryItems(ViewGroup container, int[] titles, int[] icons) {
        List<View> items = new ArrayList<>();
        collectCategoryItems(container, items);
        int count = Math.min(items.size(), titles.length);
        for (int i = 0; i < count; i++) {
            View item = items.get(i);
            TextView title = item.findViewById(R.id.homeCategoryTitle);
            ImageView icon = item.findViewById(R.id.homeCategoryIcon);
            if (title != null) {
                title.setText(titles[i]);
            }
            if (icon != null) {
                icon.setImageResource(icons[i]);
            }
            String slug = null;
            if (i == 0) slug = "laptop";
            else if (i == 1) slug = "pc";
            else if (i == 2) slug = "man-hinh";
            else if (i == 3) slug = "linh-kien";

            final String finalSlug = slug;
            item.setOnClickListener(v -> {
                if (finalSlug == null) {
                    startActivity(new Intent(this, CategoriesActivity.class));
                } else {
                    Intent intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra("category", finalSlug);
                    startActivity(intent);
                }
            });
        }
    }

    private void collectCategoryItems(View view, List<View> items) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            boolean hasDirectTitle = false;
            boolean hasDirectIcon = false;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getId() == R.id.homeCategoryTitle) {
                    hasDirectTitle = true;
                } else if (child.getId() == R.id.homeCategoryIcon) {
                    hasDirectIcon = true;
                }
            }
            if (hasDirectTitle && hasDirectIcon) {
                items.add(view);
                return;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                collectCategoryItems(group.getChildAt(i), items);
            }
        }
    }

    private void openProductDetail() {
        startActivity(new Intent(this, ProductDetailActivity.class));
    }

    private void openCategoryProducts(String categoryId, String categoryName) {
        Intent intent = new Intent(this, AuraProductsActivity.class);
        intent.putExtra(AuraProductsActivity.EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(AuraProductsActivity.EXTRA_CATEGORY_NAME, categoryName);
        startActivity(intent);
    }

    private void openProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.putExtra(ProductSearchActivity.EXTRA_SOURCE, ProductSearchActivity.SOURCE_HOME);
        startActivity(intent);
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private void addProductToCart(ProductEntity product) {
        viewModel.addProductToCart(product);
    }
}
