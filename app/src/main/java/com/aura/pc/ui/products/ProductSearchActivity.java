package com.aura.pc.ui.products;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.dao.SearchHistoryDao;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.db.entity.SearchHistoryEntity;
import com.example.aura_pc_app.data.repository.AppRepository;
import com.example.aura_pc_app.utils.LocaleManager;
import com.example.aura_pc_app.utils.ProductSearchUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductSearchActivity extends AppCompatActivity {
    public static final String EXTRA_SOURCE = "extra_source";
    public static final String SOURCE_HOME = "home";
    public static final String SOURCE_CATEGORIES = "categories";

    private static final int HISTORY_LIMIT = 5;
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService historyExecutor = Executors.newSingleThreadExecutor();
    private final List<ProductEntity> allProducts = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();
    private final List<SearchTrendAdapter.TrendItem> trendItems = new ArrayList<>();

    private SearchView searchView;
    private View trendSection;
    private View suggestionSection;
    private SearchTrendAdapter trendAdapter;
    private SearchSuggestionAdapter suggestionAdapter;
    private SearchHistoryDao searchHistoryDao;
    private Runnable pendingSearchRunnable;
    private String activeQuery = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.aura_orange));
        setContentView(R.layout.activity_product_search);

        searchHistoryDao = AppDatabase.getInstance(this).searchHistoryDao();
        allProducts.addAll(buildFallbackProducts());
        trendItems.addAll(buildCategoryTrends());

        setupViews();
        loadSearchHistory();
        observeProducts();
        showTrends();
        focusSearchInput();
    }

    private void setupViews() {
        searchView = findViewById(R.id.fullProductSearchView);
        trendSection = findViewById(R.id.trendSection);
        suggestionSection = findViewById(R.id.searchSuggestionSection);

        findViewById(R.id.searchBackButton).setOnClickListener(v -> finish());

        trendAdapter = new SearchTrendAdapter(item -> launchProductList(item.title));
        androidx.recyclerview.widget.RecyclerView trendRecycler = findViewById(R.id.trendRecyclerView);
        trendRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        trendRecycler.setAdapter(trendAdapter);
        trendRecycler.setNestedScrollingEnabled(false);
        trendAdapter.submitList(trendItems);

        suggestionAdapter = new SearchSuggestionAdapter(this::selectSuggestion);
        androidx.recyclerview.widget.RecyclerView suggestionRecycler = findViewById(R.id.searchSuggestionList);
        suggestionRecycler.setLayoutManager(new LinearLayoutManager(this));
        suggestionRecycler.setAdapter(suggestionAdapter);
        suggestionRecycler.setNestedScrollingEnabled(false);

        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint(getString(R.string.search_page_hint));
        styleSearchView(searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                launchProductList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scheduleSuggestions(newText);
                return true;
            }
        });
    }

    private void styleSearchView(SearchView targetSearchView) {
        View searchPlate = targetSearchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView queryText = targetSearchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (queryText != null) {
            queryText.setTextColor(getColor(R.color.aura_ink));
            queryText.setHintTextColor(getColor(R.color.aura_placeholder));
            queryText.setTextSize(18);
        }

        tintSearchIcon(targetSearchView, androidx.appcompat.R.id.search_mag_icon, R.color.aura_black);
        tintSearchIcon(targetSearchView, androidx.appcompat.R.id.search_close_btn, R.color.aura_placeholder);
    }

    private void tintSearchIcon(SearchView targetSearchView, int viewId, int colorRes) {
        ImageView icon = targetSearchView.findViewById(viewId);
        if (icon != null) {
            icon.setColorFilter(getColor(colorRes));
        }
    }

    private void focusSearchInput() {
        searchView.postDelayed(() -> {
            searchView.requestFocus();
            AutoCompleteTextView queryText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (queryText != null) {
                queryText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(queryText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 180);
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
            updateSearchState(activeQuery);
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

    private void scheduleSuggestions(String rawQuery) {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        pendingSearchRunnable = () -> updateSearchState(rawQuery);
        searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
    }

    private void updateSearchState(String rawQuery) {
        activeQuery = ProductSearchUtils.sanitizeKeyword(rawQuery);
        if (activeQuery.isEmpty()) {
            showTrends();
            return;
        }
        showSuggestions(activeQuery);
    }

    private void showTrends() {
        trendSection.setVisibility(View.VISIBLE);
        suggestionSection.setVisibility(View.GONE);
        suggestionAdapter.submitList(new ArrayList<>(), "");
    }

    private void showSuggestions(String query) {
        List<String> suggestions = buildKeywordSuggestions(query);
        trendSection.setVisibility(View.GONE);
        suggestionSection.setVisibility(View.VISIBLE);
        suggestionAdapter.submitList(suggestions, query);
    }

    private List<String> buildKeywordSuggestions(String query) {
        Set<String> suggestions = new LinkedHashSet<>();

        for (String recent : recentSearches) {
            if (ProductSearchUtils.contains(recent, query)) {
                suggestions.add(recent);
            }
        }

        for (ProductEntity product : allProducts) {
            if (ProductSearchUtils.matches(product, query)) {
                String name = product.name == null ? "" : product.name.trim();
                if (!name.isEmpty()) {
                    suggestions.add(name);
                }
            }
            if (suggestions.size() >= 8) {
                break;
            }
        }

        for (SearchTrendAdapter.TrendItem item : trendItems) {
            if (ProductSearchUtils.contains(item.title, query)) {
                suggestions.add(item.title);
            }
            if (suggestions.size() >= 8) {
                break;
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add(query);
        }
        return new ArrayList<>(suggestions);
    }

    private void selectSuggestion(String keyword) {
        launchProductList(keyword);
    }

    private void launchProductList(String rawQuery) {
        String keyword = ProductSearchUtils.sanitizeKeyword(rawQuery);
        if (keyword.isEmpty()) {
            return;
        }
        saveSearchQuery(keyword);
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra(ProductListActivity.EXTRA_INITIAL_QUERY, keyword);
        intent.putExtra(ProductListActivity.EXTRA_FROM_SEARCH, true);
        startActivity(intent);
        finish();
    }

    private List<SearchTrendAdapter.TrendItem> buildCategoryTrends() {
        List<SearchTrendAdapter.TrendItem> items = new ArrayList<>();
        TypedArray arrays = getResources().obtainTypedArray(R.array.category_group_item_arrays);
        for (int groupIndex = 0; groupIndex < arrays.length(); groupIndex++) {
            int arrayResId = arrays.getResourceId(groupIndex, 0);
            if (arrayResId == 0) {
                continue;
            }
            String[] names = getResources().getStringArray(arrayResId);
            for (String name : names) {
                items.add(new SearchTrendAdapter.TrendItem(name, resolveCategoryImage(groupIndex, name)));
            }
        }
        arrays.recycle();
        return items;
    }

    private int resolveCategoryImage(int groupIndex, String name) {
        String normalized = ProductSearchUtils.normalize(name);
        if (normalized.contains("laptop") || groupIndex == 2) {
            return R.drawable.figma_cat_laptop;
        }
        if (normalized.contains("pc") || groupIndex == 5) {
            return R.drawable.figma_cat_gaming_pc;
        }
        if (normalized.contains("man hinh") || normalized.contains("monitor") || groupIndex == 4) {
            return R.drawable.figma_cat_monitor;
        }
        if (normalized.contains("ban phim") || normalized.contains("keyboard")) {
            return R.drawable.figma_cat_keyboard;
        }
        if (normalized.contains("chuot") || normalized.contains("mouse")) {
            return R.drawable.figma_product_mouse;
        }
        if (normalized.contains("ssd") || normalized.contains("hdd")) {
            return R.drawable.ic_ssd;
        }
        if (normalized.contains("ram")) {
            return R.drawable.ic_ram;
        }
        if (normalized.contains("cpu")) {
            return R.drawable.ic_cpu;
        }
        if (normalized.contains("vga") || normalized.contains("gpu")) {
            return R.drawable.ic_gpu;
        }
        return groupIndex == 1 ? R.drawable.figma_cat_keyboard : R.drawable.ic_categories;
    }

    private void loadSearchHistory() {
        historyExecutor.execute(() -> {
            List<SearchHistoryEntity> entities = searchHistoryDao.getRecent(HISTORY_LIMIT);
            List<String> keywords = toKeywords(entities);
            runOnUiThread(() -> {
                recentSearches.clear();
                recentSearches.addAll(keywords);
                updateSearchState(activeQuery);
            });
        });
    }

    private void saveSearchQuery(String keyword) {
        String safeKeyword = ProductSearchUtils.sanitizeKeyword(keyword);
        if (safeKeyword.isEmpty()) {
            return;
        }
        historyExecutor.execute(() -> {
            searchHistoryDao.upsert(new SearchHistoryEntity(
                    ProductSearchUtils.normalize(safeKeyword),
                    safeKeyword,
                    System.currentTimeMillis()
            ));
            searchHistoryDao.pruneToLimit(HISTORY_LIMIT);
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

    private List<ProductEntity> buildFallbackProducts() {
        List<ProductEntity> products = new ArrayList<>();
        products.add(product("demo-1", "Laptop Gaming Acer Nitro V 16S", "Acer", "laptop-gaming-acer-nitro-v16s", 28990000, 24590000.0, "RTX 4060,16GB RAM", "laptop"));
        products.add(product("demo-2", "Man hinh ASUS ROG Strix 27 2K 170Hz", "ASUS", "man-hinh-asus-rog-strix-27", 8990000, 7990000.0, "2K 170Hz,IPS", "monitor"));
        products.add(product("demo-3", "Logitech MX Master 3S Wireless Mouse", "Logitech", "logitech-mx-master-3s", 2490000, 2190000.0, "Wireless,USB-C", "mouse"));
        products.add(product("demo-4", "PC Gaming Aura Storm RTX 4070", "AuraPC", "pc-gaming-aura-storm-rtx-4070", 38990000, 34990000.0, "RTX 4070,32GB RAM", "pc-gaming"));
        products.add(product("demo-5", "SSD Samsung 990 Pro 1TB NVMe", "Samsung", "ssd-samsung-990-pro-1tb", 3490000, 2990000.0, "NVMe Gen4,1TB", "ssd"));
        products.add(product("demo-6", "Ban phim co Keychron K8 Pro", "Keychron", "keychron-k8-pro", 2590000, null, "Bluetooth,Hot-swap", "keyboard"));
        products.add(product("demo-7", "Laptop ASUS TUF Gaming A15", "ASUS", "laptop-asus-tuf-gaming-a15", 25990000, 22990000.0, "RTX 4050,144Hz", "laptop"));
        products.add(product("demo-8", "RAM Kingston Fury Beast 16GB", "Kingston", "ram-kingston-fury-beast-16gb", 1290000, 1090000.0, "DDR5,5200MHz", "ram"));
        products.add(product("demo-9", "CPU Intel Core i7 14700K", "Intel", "cpu-intel-core-i7-14700k", 10990000, null, "20 cores,LGA1700", "cpu"));
        products.add(product("demo-10", "VGA MSI GeForce RTX 4060 Ventus", "MSI", "vga-msi-rtx-4060-ventus", 8990000, 8290000.0, "RTX 4060,8GB", "vga"));
        return products;
    }

    private ProductEntity product(String id, String name, String brand, String slug,
                                  double price, @Nullable Double salePrice, String specs,
                                  String categoryId) {
        ProductEntity product = new ProductEntity();
        product._id = id;
        product.name = name;
        product.brand = brand;
        product.slug = slug;
        product.price = price;
        product.salePrice = salePrice;
        product.specs = specs;
        product.category_id = categoryId;
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
