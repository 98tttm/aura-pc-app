package com.example.aura_pc_app.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.ui.categories.CategoriesActivity;
import com.aura.pc.ui.products.ProductListActivity;
import com.aura.pc.ui.products.ProductSearchActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.adapter.HomeProductAdapter;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.dao.SearchHistoryDao;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.db.entity.SearchHistoryEntity;
import com.example.aura_pc_app.databinding.ActivityHomeBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;
import com.example.aura_pc_app.utils.AuthGate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private HomeProductAdapter searchResultAdapter;
    private HomeSearchSuggestionAdapter suggestionAdapter;
    private HomeViewModel viewModel;
    private SearchHistoryDao searchHistoryDao;
    private Runnable pendingSearchRunnable;
    private String activeHomeQuery = "";
    private String committedSuggestionQuery = "";
    private boolean isSearchActivated;
    private boolean hideSuggestionsForCommittedQuery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_HOME);

        searchHistoryDao = AppDatabase.getInstance(this).searchHistoryDao();
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupProductList();
        setupHomeSearch();
        setupHomeActions();

        allProducts.addAll(createFallbackProducts());
        productAdapter.setProducts(allProducts);
        observeProducts();
    }

    @Override
    protected ActivityHomeBinding inflateBinding() {
        return ActivityHomeBinding.inflate(getLayoutInflater());
    }

    private void setupProductList() {
        HomeProductAdapter.ProductClickListener listener = new HomeProductAdapter.ProductClickListener() {
            @Override
            public void onProductClick(ProductEntity product) {
                if (!activeHomeQuery.isEmpty()) {
                    saveSearchQuery(activeHomeQuery);
                }
                openProductDetail();
            }

            @Override
            public void onCartClick(ProductEntity product) {
                addProductToCart();
            }
        };

        productAdapter = new HomeProductAdapter(listener);
        binding.productRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.productRecyclerView.setAdapter(productAdapter);
        binding.productRecyclerView.setHasFixedSize(false);

        searchResultAdapter = new HomeProductAdapter(listener);
        binding.homeSearchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.homeSearchResultsRecyclerView.setAdapter(searchResultAdapter);
        binding.homeSearchResultsRecyclerView.setHasFixedSize(false);
        binding.homeSearchResultsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void setupHomeSearch() {
        binding.homeProductSearchView.setIconifiedByDefault(false);
        binding.homeProductSearchView.setSubmitButtonEnabled(false);
        binding.homeProductSearchView.setQueryHint(getString(R.string.home_figma_search_hint));
        styleSearchView(binding.homeProductSearchView);
        binding.homeProductSearchView.clearFocus();
        binding.homeProductSearchView.setFocusable(false);
        binding.homeProductSearchView.setFocusableInTouchMode(false);
        binding.homeProductSearchView.setOnClickListener(v -> openProductSearch());
        binding.homeSearchContainer.setOnClickListener(v -> openProductSearch());

        AutoCompleteTextView queryText = binding.homeProductSearchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (queryText != null) {
            queryText.setFocusable(false);
            queryText.setFocusableInTouchMode(false);
            queryText.setCursorVisible(false);
            queryText.setKeyListener(null);
            queryText.setOnClickListener(v -> openProductSearch());
        }

        binding.homeSearchSuggestionPanel.setVisibility(View.GONE);
        binding.homeSearchResultsSection.setVisibility(View.GONE);
    }

    private void styleSearchView(SearchView searchView) {
        View searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView queryText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (queryText != null) {
            queryText.setTextColor(getColor(R.color.aura_ink));
            queryText.setHintTextColor(getColor(R.color.aura_placeholder));
            queryText.setTextSize(14);
        }

        tintSearchIcon(searchView, androidx.appcompat.R.id.search_mag_icon);
        tintSearchIcon(searchView, androidx.appcompat.R.id.search_close_btn);
    }

    private void tintSearchIcon(SearchView searchView, int viewId) {
        ImageView icon = searchView.findViewById(viewId);
        if (icon != null) {
            icon.setColorFilter(getColor(R.color.aura_placeholder));
        }
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
        binding.homeQuickBuilder.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_builder_pending, Toast.LENGTH_SHORT).show());
        binding.homeViewAllProducts.setOnClickListener(v -> openProductList());
        setupCategoryStrip();
    }

    private void observeProducts() {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        viewModel.getProducts().observe(this, products -> {
            List<ProductEntity> displayProducts = products == null || products.isEmpty()
                    ? createFallbackProducts()
                    : products;
            allProducts.clear();
            allProducts.addAll(displayProducts);
            productAdapter.setProducts(displayProducts);
            binding.loadingProgress.setVisibility(View.GONE);
        });
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
                R.drawable.figma_cat_monitor,
                R.drawable.figma_cat_keyboard,
                R.drawable.figma_product_mouse,
                R.drawable.ic_categories
        };

        int count = Math.min(binding.homeCategoryStrip.getChildCount(), titles.length);
        for (int i = 0; i < count; i++) {
            View item = binding.homeCategoryStrip.getChildAt(i);
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

    private void openProductDetail() {
        startActivity(new Intent(this, MainActivity.class));
    }

    private void openProductList() {
        startActivity(new Intent(this, ProductListActivity.class));
    }

    private void openProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.putExtra(ProductSearchActivity.EXTRA_SOURCE, ProductSearchActivity.SOURCE_HOME);
        startActivity(intent);
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private void addProductToCart() {
        if (!AuthGate.requireLogin(this, CartActivity.class)) {
            return;
        }
        openCart();
    }

    private List<ProductEntity> createFallbackProducts() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(product("fallback-acer-nitro", "Laptop Gaming Acer Nitro V 16S", "Acer",
                "laptop-gaming-acer-nitro-v16s", 28990000, 24590000.0, "RTX 4060,16GB RAM"));
        products.add(product("fallback-aura-obsidian", "Aura Obsidian Pro X", "AuraPC",
                "aura-obsidian-pro-x", 100500000, 85000000.0, "RTX 4090,64GB RAM"));
        products.add(product("fallback-pc-ai", "Aura PC AI Creator", "AuraPC",
                "aura-pc-ai-creator", 52990000, 48990000.0, "RTX 4070,32GB RAM"));
        products.add(product("fallback-monitor", "Man hinh Gaming 27 inch", "ASUS",
                "man-hinh-gaming-27-inch", 7990000, 6490000.0, "2K 170Hz,IPS"));
        products.add(product("fallback-keyboard", "Ban phim co Aura RGB", "AuraPC",
                "ban-phim-co-aura-rgb", 1890000, 1490000.0, "Mechanical,RGB"));
        products.add(product("fallback-ssd", "SSD NVMe Gen4 1TB", "Samsung",
                "ssd-nvme-gen4-1tb", 2490000, 1990000.0, "NVMe Gen4,1TB"));
        return products;
    }

    private ProductEntity product(String id, String name, String brand, String slug,
                                  double price, @Nullable Double salePrice, String specs) {
        ProductEntity product = new ProductEntity();
        product._id = id;
        product.name = name;
        product.brand = brand;
        product.slug = slug;
        product.price = price;
        product.salePrice = salePrice;
        product.specs = specs;
        product.active = true;
        product.stock = 12;
        return product;
    }

    @Override
    protected void onDestroy() {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        historyExecutor.shutdownNow();
        super.onDestroy();
    }
}
