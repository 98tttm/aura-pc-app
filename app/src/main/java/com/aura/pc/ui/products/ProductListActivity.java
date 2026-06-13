package com.aura.pc.ui.products;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.dao.SearchHistoryDao;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.db.entity.SearchHistoryEntity;
import com.example.aura_pc_app.data.repository.AppRepository;
import com.example.aura_pc_app.databinding.ActivityAuraProductsBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;
import com.example.aura_pc_app.utils.ProductSearchUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductListActivity extends BaseActivity<ActivityAuraProductsBinding> {
    public static final String EXTRA_INITIAL_QUERY = "extra_initial_query";
    public static final String EXTRA_FROM_SEARCH = "extra_from_search";

    private static final int DEFAULT_VISIBLE_PRODUCTS = 8;
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

    private ProductListAdapter productAdapter;
    private SearchSuggestionAdapter suggestionAdapter;
    private SearchHistoryDao searchHistoryDao;
    private Runnable pendingSearchRunnable;
    private String activeQuery = "";
    private String committedSuggestionQuery = "";
    private boolean hideSuggestionsForCommittedQuery;
    private boolean isSearchActivated;
    private int visibleProductCount = DEFAULT_VISIBLE_PRODUCTS;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.aura_orange));

        searchHistoryDao = AppDatabase.getInstance(this).searchHistoryDao();
        setupProductList();
        setupSearchSuggestions();
        setupSearchView();
        setupActions();
        setupLoadMoreButton();
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);

        // Giành focus ban đầu để SearchView không tự bật bàn phím + popup khi mới vào màn hình.
        binding.productSearchContainer.setFocusableInTouchMode(true);
        binding.productSearchContainer.requestFocus();

        allProducts.addAll(buildFallbackProducts());
        String initialQuery = sanitizeKeyword(getIntent().getStringExtra(EXTRA_INITIAL_QUERY));
        if (initialQuery.isEmpty()) {
            applySearch("");
        } else {
            hideSuggestionsForCommittedQuery = true;
            committedSuggestionQuery = initialQuery;
            binding.productSearchView.setQuery(initialQuery, false);
            runSearchImmediately(initialQuery, true);
            binding.productSearchView.clearFocus();
            hideSuggestionPanel();
        }
        loadSearchHistory();
        observeProducts();
    }

    @Override
    protected ActivityAuraProductsBinding inflateBinding() {
        return ActivityAuraProductsBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Khi quay lại màn hình Danh mục sản phẩm: trả về trạng thái ban đầu, không còn popup tìm kiếm.
        isSearchActivated = false;
        binding.productSearchView.clearFocus();
        binding.productSearchContainer.requestFocus();
        hideSuggestionPanel();
    }

    private void setupProductList() {
        productAdapter = new ProductListAdapter(new ProductListAdapter.ProductActionListener() {
            @Override
            public void onProductClick(ProductEntity product) {
                if (!activeQuery.isEmpty()) {
                    saveSearchQuery(activeQuery);
                }
                openProductDetail();
            }

            @Override
            public void onCartClick(ProductEntity product) {
                startActivity(new Intent(ProductListActivity.this, CartActivity.class));
            }
        });

        binding.productRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.productRecyclerView.setAdapter(productAdapter);
        binding.productRecyclerView.setNestedScrollingEnabled(false);
        binding.productRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                int side = dp(8);
                outRect.left = position % 2 == 0 ? 0 : side;
                outRect.right = position % 2 == 0 ? side : 0;
                outRect.bottom = dp(27);
            }
        });
    }

    private void setupSearchSuggestions() {
        suggestionAdapter = new SearchSuggestionAdapter(this::selectSuggestion);
        binding.searchSuggestionList.setLayoutManager(new LinearLayoutManager(this));
        binding.searchSuggestionList.setAdapter(suggestionAdapter);
        binding.searchSuggestionList.setNestedScrollingEnabled(false);
    }

    private void setupSearchView() {
        binding.productSearchView.setIconifiedByDefault(false);
        binding.productSearchView.setSubmitButtonEnabled(false);
        binding.productSearchView.setQueryHint(getString(R.string.search_hint));
        styleSearchView(binding.productSearchView);
        binding.productSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                runSearchImmediately(query, true);
                binding.productSearchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scheduleDebouncedSearch(newText);
                return true;
            }
        });
        // Chỉ coi là "đang tìm kiếm" khi người dùng thực sự focus vào ô tìm kiếm,
        // nhờ vậy panel gợi ý không tự bật khi mới vào / quay lại màn hình.
        binding.productSearchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                isSearchActivated = true;
                updateSuggestions(activeQuery);
            }
        });
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
            queryText.setTextSize(16);
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

    private void setupActions() {
        binding.topCartButton.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        binding.topNotificationsButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        binding.searchFilterButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
        binding.contextualFab.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
    }

    private void setupLoadMoreButton() {
        binding.loadMoreProductsButton.setOnClickListener(v -> {
            visibleProductCount += DEFAULT_VISIBLE_PRODUCTS;
            applySearch(activeQuery);
        });
    }

    private void observeProducts() {
        AppRepository repository = new AppRepository(
                getApplication(),
                ApiClient.getInstance(this).getApiService()
        );
        repository.getProducts().observe(this, products -> {
            if (products == null || products.isEmpty()) {
                return;
            }
            mergeProducts(products);
            applySearch(activeQuery);
        });
    }

    private void mergeProducts(List<ProductEntity> products) {
        Set<String> seen = new LinkedHashSet<>();
        List<ProductEntity> merged = new ArrayList<>();
        addProducts(merged, seen, products);
        addProducts(merged, seen, buildFallbackProducts());
        allProducts.clear();
        allProducts.addAll(merged);
    }

    private void addProducts(List<ProductEntity> target, Set<String> seen, List<ProductEntity> products) {
        for (ProductEntity product : products) {
            String key = product._id == null || product._id.trim().isEmpty()
                    ? ProductSearchUtils.normalize(product.name)
                    : product._id;
            if (seen.add(key)) {
                target.add(product);
            }
        }
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
        pendingSearchRunnable = () -> {
            visibleProductCount = DEFAULT_VISIBLE_PRODUCTS;
            applySearch(rawQuery);
        };
        searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
    }

    private void selectSuggestion(String keyword) {
        String selectedQuery = sanitizeKeyword(keyword);
        if (selectedQuery.isEmpty()) {
            return;
        }

        hideSuggestionsForCommittedQuery = true;
        committedSuggestionQuery = selectedQuery;
        binding.productSearchView.setQuery(selectedQuery, false);
        runSearchImmediately(selectedQuery, true);
        binding.productSearchView.clearFocus();
        hideSuggestionPanel();
    }

    private void runSearchImmediately(String rawQuery, boolean persist) {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        visibleProductCount = DEFAULT_VISIBLE_PRODUCTS;
        applySearch(rawQuery);
        if (persist) {
            saveSearchQuery(rawQuery);
        }
    }

    private void applySearch(String rawQuery) {
        activeQuery = sanitizeKeyword(rawQuery);
        List<ProductEntity> filtered = filterProducts(activeQuery);
        int visibleCount = Math.min(visibleProductCount, filtered.size());
        List<ProductEntity> visibleProducts = new ArrayList<>(filtered.subList(0, visibleCount));

        productAdapter.submitList(visibleProducts, activeQuery);
        binding.productEmptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        updateLoadMoreButton(filtered.size() - visibleCount);
        updateSuggestions(activeQuery);
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
        return ProductSearchUtils.matches(product, query);
    }

    private boolean contains(String value, String lowerQuery) {
        return ProductSearchUtils.contains(value, lowerQuery);
    }

    private void updateSuggestions(String query) {
        // Chưa chủ động tìm kiếm -> giữ màn hình ở trạng thái ban đầu, không có popup gợi ý.
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
            binding.searchSuggestionTitle.setText(R.string.search_popular_title);
            suggestions = buildIdleSuggestions();
        } else {
            binding.searchSuggestionTitle.setText(R.string.search_suggestion_title);
            suggestions = buildKeywordSuggestions(query);
        }
        binding.searchSuggestionPanel.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
        suggestionAdapter.submitList(suggestions, query);
    }

    private void hideSuggestionPanel() {
        binding.searchSuggestionPanel.setVisibility(View.GONE);
        suggestionAdapter.submitList(new ArrayList<>(), activeQuery);
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
            if (ProductSearchUtils.contains(popular, lowerQuery)) {
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
                updateSuggestions(activeQuery);
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
                updateSuggestions(activeQuery);
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

    private void updateLoadMoreButton(int remainingProducts) {
        if (remainingProducts <= 0) {
            binding.loadMoreProductsButton.setVisibility(View.GONE);
            return;
        }
        binding.loadMoreProductsButton.setVisibility(View.VISIBLE);
        String text = getString(R.string.product_list_load_more, remainingProducts);
        binding.loadMoreProductsButton.setText(text);
        binding.loadMoreProductsButton.setContentDescription(text);
    }

    private String sanitizeKeyword(String rawQuery) {
        return ProductSearchUtils.sanitizeKeyword(rawQuery);
    }

    private String normalizeKeyword(String keyword) {
        return sanitizeKeyword(keyword).toLowerCase(Locale.ROOT);
    }

    private List<ProductEntity> buildFallbackProducts() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(product("demo-1", "Laptop Gaming Acer Nitro V 16S", "Acer", "laptop-gaming-acer-nitro-v16s", 28990000, 24590000.0, "RTX 4060,16GB RAM"));
        products.add(product("demo-2", "Man hinh ASUS ROG Strix 27 2K 170Hz", "ASUS", "man-hinh-asus-rog-strix-27", 8990000, 7990000.0, "2K 170Hz,IPS"));
        products.add(product("demo-3", "Logitech MX Master 3S Wireless Mouse", "Logitech", "logitech-mx-master-3s", 2490000, 2190000.0, "Wireless,USB-C"));
        products.add(product("demo-4", "PC Gaming Aura Storm RTX 4070", "AuraPC", "pc-gaming-aura-storm-rtx-4070", 38990000, 34990000.0, "RTX 4070,32GB RAM"));
        products.add(product("demo-5", "SSD Samsung 990 Pro 1TB NVMe", "Samsung", "ssd-samsung-990-pro-1tb", 3490000, 2990000.0, "NVMe Gen4,1TB"));
        products.add(product("demo-6", "Ban phim co Keychron K8 Pro", "Keychron", "keychron-k8-pro", 2590000, null, "Bluetooth,Hot-swap"));
        products.add(product("demo-7", "Laptop ASUS TUF Gaming A15", "ASUS", "laptop-asus-tuf-gaming-a15", 25990000, 22990000.0, "RTX 4050,144Hz"));
        products.add(product("demo-8", "RAM Kingston Fury Beast 16GB", "Kingston", "ram-kingston-fury-beast-16gb", 1290000, 1090000.0, "DDR5,5200MHz"));
        products.add(product("demo-9", "CPU Intel Core i7 14700K", "Intel", "cpu-intel-core-i7-14700k", 10990000, null, "20 cores,LGA1700"));
        products.add(product("demo-10", "VGA MSI GeForce RTX 4060 Ventus", "MSI", "vga-msi-rtx-4060-ventus", 8990000, 8290000.0, "RTX 4060,8GB"));
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

    private void bindCartButtons(View view) {
        if (view instanceof ImageButton) {
            CharSequence description = view.getContentDescription();
            if (description != null && description.equals(getString(R.string.cd_add_to_cart))) {
                view.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
            }
            return;
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            bindCartButtons(group.getChildAt(i));
        }
    }

    private void openProductDetail() {
        startActivity(new Intent(this, MainActivity.class));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
