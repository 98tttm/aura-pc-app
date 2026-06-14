package com.aura.pc.ui.products;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AppCompatActivity;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.bumptech.glide.Glide;
import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.utils.LocaleManager;

import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuraProductsActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";
    public static final String EXTRA_SELECTED_FILTER_TYPE = "extra_selected_filter_type";
    public static final String EXTRA_SELECTED_FILTER_LABEL = "extra_selected_filter_label";
    public static final String EXTRA_PRICE_MIN = "extra_price_min";
    public static final String EXTRA_PRICE_MAX = "extra_price_max";
    public static final String FILTER_TYPE_FEATURE = "feature";
    public static final String FILTER_TYPE_PRICE = "price";
    private static final int SORT_POPULAR = 0;
    private static final int SORT_PROMOTION = 1;
    private static final int SORT_PRICE = 2;

    private final List<CategoryItem> categories = new ArrayList<>();
    private final Map<String, List<CategoryItem>> childrenByParent = new HashMap<>();
    private GridLayout productGrid;
    private ProgressBar loadingProgress;
    private TextView categoryTitleText;
    private TextView breadcrumbText;
    private TextView sortPopularButton;
    private TextView sortPromotionButton;
    private TextView sortPriceButton;
    private View sortActiveIndicator;
    private LinearLayout subcategoryChipRow;
    private NestedScrollView productScrollView;
    private View stickyCollapsedHeader;
    private final List<Map<String, Object>> loadedProducts = new ArrayList<>();
    private NumberFormat currencyFormat;
    private String categoryId;
    private String categoryName;
    private String selectedFilterType;
    private String selectedFilterLabel;
    private double selectedPriceMin;
    private double selectedPriceMax;
    private CategoryItem selectedCategory;
    private CategoryItem selectedRoot;
    private boolean stickyHeaderVisible;
    private int activeSort = SORT_POPULAR;
    private boolean priceAscending = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aura_products);

        currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        selectedFilterType = getIntent().getStringExtra(EXTRA_SELECTED_FILTER_TYPE);
        selectedFilterLabel = getIntent().getStringExtra(EXTRA_SELECTED_FILTER_LABEL);
        selectedPriceMin = getIntent().getDoubleExtra(EXTRA_PRICE_MIN, 0);
        selectedPriceMax = getIntent().getDoubleExtra(EXTRA_PRICE_MAX, 0);

        bindViews();
        setupActions();
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);
        updateCategoryHeader();
        updateSortUi();
        renderFallbackChips();
        loadCategories();
        loadProducts();
    }

    private void bindViews() {
        productGrid = findViewById(R.id.productGrid);
        loadingProgress = findViewById(R.id.loadingProgress);
        categoryTitleText = findViewById(R.id.categoryTitleText);
        breadcrumbText = findViewById(R.id.breadcrumbText);
        sortPopularButton = findViewById(R.id.sortPopularButton);
        sortPromotionButton = findViewById(R.id.sortPromotionButton);
        sortPriceButton = findViewById(R.id.sortPriceButton);
        sortActiveIndicator = findViewById(R.id.sortActiveIndicator);
        subcategoryChipRow = findViewById(R.id.subcategoryChipRow);
        productScrollView = findViewById(R.id.productScrollView);
        stickyCollapsedHeader = findViewById(R.id.stickyCollapsedHeader);
        setSearchHints();
    }

    private void setupActions() {
        View menu = findViewById(R.id.topMenuButton);
        View fixedMenu = findViewById(R.id.fixedTopMenuButton);
        View back = findViewById(R.id.productBackButton);
        View stickyMenu = findViewById(R.id.stickyTopMenuButton);
        View topSearch = findViewById(R.id.topSearchBox);
        View fixedSearch = findViewById(R.id.fixedTopSearchBox);
        View stickySearch = findViewById(R.id.stickySearchBox);
        View filter = findViewById(R.id.searchFilterButton);
        View fab = findViewById(R.id.contextualFab);
        View notifications = findViewById(R.id.topNotificationsButton);
        View fixedNotifications = findViewById(R.id.fixedTopNotificationsButton);
        View stickyNotifications = findViewById(R.id.stickyNotificationsButton);
        View cart = findViewById(R.id.topCartButton);
        View fixedCart = findViewById(R.id.fixedTopCartButton);
        View stickyCart = findViewById(R.id.stickyCartButton);

        if (menu != null) menu.setOnClickListener(v -> finish());
        if (fixedMenu != null) fixedMenu.setOnClickListener(v -> finish());
        if (back != null) back.setOnClickListener(v -> finish());
        if (stickyMenu != null) stickyMenu.setOnClickListener(v -> finish());
        if (topSearch != null) topSearch.setOnClickListener(v -> focusSearch(R.id.searchInput));
        if (fixedSearch != null) fixedSearch.setOnClickListener(v -> focusSearch(R.id.fixedSearchInput));
        if (stickySearch != null) stickySearch.setOnClickListener(v -> focusSearch(R.id.stickySearchInput));
        if (filter != null) filter.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
        if (fab != null) fab.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
        if (notifications != null) notifications.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        if (fixedNotifications != null) fixedNotifications.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        if (stickyNotifications != null) stickyNotifications.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        if (cart != null) cart.setOnClickListener(v -> openCart());
        if (fixedCart != null) fixedCart.setOnClickListener(v -> openCart());
        if (stickyCart != null) stickyCart.setOnClickListener(v -> openCart());
        if (sortPopularButton != null) sortPopularButton.setOnClickListener(v -> {
            activeSort = SORT_POPULAR;
            renderCurrentProducts();
        });
        if (sortPromotionButton != null) sortPromotionButton.setOnClickListener(v -> {
            activeSort = SORT_PROMOTION;
            renderCurrentProducts();
        });
        if (sortPriceButton != null) sortPriceButton.setOnClickListener(v -> {
            if (activeSort == SORT_PRICE) {
                priceAscending = !priceAscending;
            } else {
                activeSort = SORT_PRICE;
                priceAscending = true;
            }
            renderCurrentProducts();
        });
    }

    private void setupStickyHeader() {
        if (productScrollView == null || stickyCollapsedHeader == null) return;
        productScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> updateStickyHeader(scrollY));
        updateStickyHeader(productScrollView.getScrollY());
    }

    private void updateStickyHeader(int scrollY) {
        if (stickyCollapsedHeader == null) return;
        boolean shouldShow = scrollY >= dpToPx(82);
        if (shouldShow == stickyHeaderVisible) return;
        stickyHeaderVisible = shouldShow;
        stickyCollapsedHeader.animate().cancel();
        if (shouldShow) {
            stickyCollapsedHeader.setVisibility(View.VISIBLE);
            stickyCollapsedHeader.setTranslationY(-dpToPx(8));
            stickyCollapsedHeader.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(140)
                    .start();
        } else {
            stickyCollapsedHeader.animate()
                    .alpha(0f)
                    .translationY(-dpToPx(8))
                    .setDuration(120)
                    .withEndAction(() -> stickyCollapsedHeader.setVisibility(View.GONE))
                    .start();
        }
    }

    private void focusSearch(int inputId) {
        EditText searchInput = findViewById(inputId);
        if (searchInput != null) {
            searchInput.requestFocus();
        }
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private void loadCategories() {
        ApiClient.getInstance(this).getApiService().getCategories().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    consumeCategories(response.body());
                    return;
                }
                showFallbackCategories();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                showFallbackCategories();
            }
        });
    }

    private void consumeCategories(List<Map<String, Object>> rawItems) {
        categories.clear();
        childrenByParent.clear();
        for (Map<String, Object> raw : rawItems) {
            CategoryItem item = CategoryItem.from(raw);
            if (item.categoryId.isEmpty() || item.name.isEmpty()) continue;
            categories.add(item);
            String parentKey = item.parentId == null ? "" : item.parentId;
            List<CategoryItem> siblings = childrenByParent.get(parentKey);
            if (siblings == null) {
                siblings = new ArrayList<>();
                childrenByParent.put(parentKey, siblings);
            }
            siblings.add(item);
        }
        for (List<CategoryItem> siblings : childrenByParent.values()) {
            Collections.sort(siblings, (a, b) -> a.name.compareToIgnoreCase(b.name));
        }

        boolean hadCategoryId = !TextUtils.isEmpty(categoryId);
        syncSelectedCategoryFromIntent();
        updateCategoryHeader();
        renderSubcategoryChips();
        if (!hadCategoryId && !TextUtils.isEmpty(categoryId)) {
            loadProducts();
        }
    }

    private void showFallbackCategories() {
        List<Map<String, Object>> fallback = new ArrayList<>();
        fallback.add(mapCategory("laptop", null, "Laptop", 1));
        fallback.add(mapCategory("pc", null, "PC", 1));
        fallback.add(mapCategory("linh-kien", null, "Linh kiện", 1));
        fallback.add(mapCategory("phu-kien", null, "Phụ kiện", 1));
        fallback.add(mapCategory("man-hinh", null, "Màn hình", 1));
        fallback.add(mapCategory("gaming-gear", null, "Gaming gear", 1));
        fallback.add(mapCategory("ban-ghe", null, "Bàn-Ghế", 1));
        fallback.add(mapCategory("laptop-asus", "laptop", "ASUS", 2));
        fallback.add(mapCategory("laptop-lenovo", "laptop", "LENOVO", 2));
        fallback.add(mapCategory("laptop-gaming-ai", "laptop", "Laptop A.I", 2));
        fallback.add(mapCategory("laptop-van-phong", "laptop", "Văn phòng", 2));
        fallback.add(mapCategory("laptop-sinh-vien", "laptop", "Sinh viên", 2));
        fallback.add(mapCategory("pc-gaming", "pc", "PC Gaming", 2));
        fallback.add(mapCategory("cpu-amd", "linh-kien", "CPU AMD", 2));
        fallback.add(mapCategory("vga", "linh-kien", "VGA", 2));
        fallback.add(mapCategory("ram", "linh-kien", "RAM", 2));
        consumeCategories(fallback);
    }

    private Map<String, Object> mapCategory(String id, String parent, String name, int level) {
        Map<String, Object> item = new HashMap<>();
        item.put("category_id", id);
        item.put("parent_id", parent);
        item.put("name", name);
        item.put("level", level);
        return item;
    }

    private void syncSelectedCategoryFromIntent() {
        selectedCategory = findById(categoryId);
        if (selectedCategory == null && !TextUtils.isEmpty(categoryId)) {
            selectedCategory = new CategoryItem(categoryId, null, nonEmpty(categoryName, categoryId), 0);
        }
        if (selectedCategory == null) {
            selectedCategory = findById("laptop");
        }
        if (selectedCategory == null && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }
        selectedRoot = findRoot(selectedCategory);
        if (selectedRoot == null) selectedRoot = selectedCategory;
        if (selectedCategory != null) {
            categoryId = selectedCategory.categoryId;
            categoryName = selectedCategory.name;
        }
    }

    private CategoryItem findRoot(CategoryItem item) {
        if (item == null) return null;
        CategoryItem current = item;
        while (current.parentId != null && !current.parentId.isEmpty()) {
            CategoryItem parent = findById(current.parentId);
            if (parent == null) break;
            current = parent;
        }
        return current;
    }

    private void updateCategoryHeader() {
        String displayName = displayCategoryName();
        String title = nonEmpty(displayName, getString(R.string.products_title));
        if (categoryTitleText != null) {
            categoryTitleText.setText(title);
        }
        setSearchHints();
        if (breadcrumbText == null) return;
        if (!TextUtils.isEmpty(selectedFilterLabel) && selectedRoot != null) {
            breadcrumbText.setText(selectedRoot.name + " / " + selectedFilterLabel);
            breadcrumbText.setVisibility(View.VISIBLE);
        } else if (selectedCategory != null
                && selectedRoot != null
                && !selectedRoot.categoryId.equals(selectedCategory.categoryId)) {
            breadcrumbText.setText(selectedRoot.name + " / " + selectedCategory.name);
            breadcrumbText.setVisibility(View.VISIBLE);
        } else {
            breadcrumbText.setVisibility(View.GONE);
        }
    }

    private void setSearchHints() {
        String displayName = displayCategoryName();
        String hint = TextUtils.isEmpty(displayName)
                ? getString(R.string.home_figma_search_hint)
                : getString(R.string.products_for_category, displayName);
        EditText searchInput = findViewById(R.id.searchInput);
        EditText fixedSearchInput = findViewById(R.id.fixedSearchInput);
        EditText stickySearchInput = findViewById(R.id.stickySearchInput);
        if (searchInput != null) searchInput.setHint(hint);
        if (fixedSearchInput != null) fixedSearchInput.setHint(hint);
        if (stickySearchInput != null) stickySearchInput.setHint(hint);
    }

    private String displayCategoryName() {
        return TextUtils.isEmpty(selectedFilterLabel) ? categoryName : selectedFilterLabel;
    }

    private void renderFallbackChips() {
        if (subcategoryChipRow == null) return;
        subcategoryChipRow.removeAllViews();
        subcategoryChipRow.addView(createChip(getString(R.string.filter_all), TextUtils.isEmpty(selectedFilterLabel), () -> clearSelectedFilter()));
        if (!TextUtils.isEmpty(selectedFilterLabel)) {
            subcategoryChipRow.addView(createChip(selectedFilterLabel, true, () -> { }));
        }
    }

    private void renderSubcategoryChips() {
        if (subcategoryChipRow == null) return;
        subcategoryChipRow.removeAllViews();
        CategoryItem chipParent = selectedRoot != null ? selectedRoot : selectedCategory;
        boolean allSelected = selectedCategory == null
                || chipParent == null
                || selectedCategory.categoryId.equals(chipParent.categoryId);
        subcategoryChipRow.addView(createChip(getString(R.string.filter_all), allSelected && TextUtils.isEmpty(selectedFilterLabel), () -> {
            clearSelectedFilter();
            selectCategory(chipParent);
        }));
        if (chipParent == null) return;
        if (!TextUtils.isEmpty(selectedFilterLabel)) {
            subcategoryChipRow.addView(createChip(selectedFilterLabel, true, () -> { }));
        }
        for (CategoryItem child : childrenOf(chipParent.categoryId)) {
            if (!TextUtils.isEmpty(selectedFilterLabel)
                    && normalizeSearchText(selectedFilterLabel).equals(normalizeSearchText(child.name))) {
                continue;
            }
            boolean selected = selectedCategory != null
                    && selectedCategory.categoryId.equals(child.categoryId)
                    && TextUtils.isEmpty(selectedFilterLabel);
            subcategoryChipRow.addView(createChip(child.name, selected, () -> selectCategory(child)));
        }
    }

    private TextView createChip(String label, boolean selected, Runnable onClick) {
        TextView chip = new TextView(this);
        chip.setLayoutParams(chipLayoutParams(subcategoryChipRow == null || subcategoryChipRow.getChildCount() == 0));
        chip.setMinWidth(dpToPx(72));
        chip.setHeight(dpToPx(32));
        chip.setGravity(Gravity.CENTER);
        chip.setIncludeFontPadding(false);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setPadding(dpToPx(18), 0, dpToPx(18), 0);
        chip.setText(label);
        chip.setTextSize(13);
        chip.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(getColor(R.color.aura_white));
        chip.setBackgroundResource(selected ? R.drawable.bg_chip_active : R.drawable.bg_chip_dark);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setOnClickListener(v -> {
            if (onClick != null) onClick.run();
        });
        return chip;
    }

    private LinearLayout.LayoutParams chipLayoutParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(32)
        );
        if (!first) params.setMarginStart(dpToPx(12));
        return params;
    }

    private void selectCategory(CategoryItem item) {
        if (item == null) return;
        clearSelectedFilter();
        selectedCategory = item;
        selectedRoot = findRoot(item);
        if (selectedRoot == null) selectedRoot = item;
        categoryId = item.categoryId;
        categoryName = item.name;
        activeSort = SORT_POPULAR;
        priceAscending = true;
        updateCategoryHeader();
        updateSortUi();
        renderSubcategoryChips();
        if (productScrollView != null) {
            productScrollView.smoothScrollTo(0, 0);
        }
        loadProducts();
    }

    private void clearSelectedFilter() {
        selectedFilterType = "";
        selectedFilterLabel = "";
        selectedPriceMin = 0;
        selectedPriceMax = 0;
    }

    private List<CategoryItem> childrenOf(String parentId) {
        List<CategoryItem> children = childrenByParent.get(parentId);
        return children == null ? new ArrayList<>() : children;
    }

    private CategoryItem findById(String id) {
        if (TextUtils.isEmpty(id)) return null;
        for (CategoryItem item : categories) {
            if (id.equals(item.categoryId)) return item;
        }
        return null;
    }

    private void loadProducts() {
        setLoading(true);
        Call<Map<String, Object>> call = TextUtils.isEmpty(categoryId)
                ? ApiClient.getInstance(this).getApiService().getProducts()
                : ApiClient.getInstance(this).getApiService().getProductsByCategory(categoryId, 1, 60);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> items = readItems(response.body());
                    loadedProducts.clear();
                    loadedProducts.addAll(items);
                    renderCurrentProducts();
                    return;
                }
                loadedProducts.clear();
                showEmptyOrError(R.string.products_load_failed);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                loadedProducts.clear();
                showEmptyOrError(R.string.products_load_failed);
            }
        });
    }

    private void renderCurrentProducts() {
        updateSortUi();
        List<Map<String, Object>> products = new ArrayList<>(loadedProducts);
        products = applySelectedFilter(products);
        if (activeSort == SORT_PROMOTION) {
            List<Map<String, Object>> saleProducts = new ArrayList<>();
            for (Map<String, Object> product : products) {
                double priceValue = firstNumber(product, "price", "sale_price", "final_price");
                double oldPriceValue = firstNumber(product, "old_price", "original_price", "compare_at_price", "market_price");
                if (discountPercent(priceValue, oldPriceValue) > 0) {
                    saleProducts.add(product);
                }
            }
            products = saleProducts;
        } else if (activeSort == SORT_PRICE) {
            Collections.sort(products, (a, b) -> {
                double left = firstNumber(a, "price", "sale_price", "final_price");
                double right = firstNumber(b, "price", "sale_price", "final_price");
                boolean leftContact = left <= 0;
                boolean rightContact = right <= 0;
                if (leftContact != rightContact) {
                    return leftContact ? 1 : -1;
                }
                int result = Double.compare(left, right);
                return priceAscending ? result : -result;
            });
        }
        renderProducts(products);
    }

    private List<Map<String, Object>> applySelectedFilter(List<Map<String, Object>> products) {
        if (TextUtils.isEmpty(selectedFilterLabel)) {
            return products;
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        if (FILTER_TYPE_PRICE.equals(selectedFilterType)) {
            double minPrice = selectedPriceMin;
            double maxPrice = selectedPriceMax;
            if (minPrice <= 0 && maxPrice <= 0) {
                double[] inferredBounds = inferPriceBounds(selectedFilterLabel);
                minPrice = inferredBounds[0];
                maxPrice = inferredBounds[1];
            }
            for (Map<String, Object> product : products) {
                double priceValue = firstNumber(product, "sale_price", "final_price", "price");
                boolean aboveMin = minPrice <= 0 || priceValue >= minPrice;
                boolean belowMax = maxPrice <= 0 || priceValue <= maxPrice;
                if (priceValue > 0 && aboveMin && belowMax) {
                    filtered.add(product);
                }
            }
            return filtered;
        }
        String needle = normalizeSearchText(selectedFilterLabel);
        for (Map<String, Object> product : products) {
            String haystack = normalizeSearchText(
                    stringValue(product.get("name"), "") + " "
                            + stringValue(product.get("brand"), "") + " "
                            + stringValue(product.get("description"), "") + " "
                            + stringValue(product.get("specs"), "") + " "
                            + stringValue(product.get("attributes"), ""));
            if (haystack.contains(needle)) {
                filtered.add(product);
            }
        }
        return filtered.isEmpty() ? products : filtered;
    }

    private double[] inferPriceBounds(String label) {
        String normalized = normalizeSearchText(label);
        if (normalized.contains("duoi") || normalized.contains("under")) return new double[]{0, 10000000};
        if (normalized.contains("tren") || normalized.contains("above")) return new double[]{50000000, 0};
        if (normalized.contains("10") && normalized.contains("15")) return new double[]{10000000, 15000000};
        if (normalized.contains("15") && normalized.contains("20")) return new double[]{15000000, 20000000};
        if (normalized.contains("20") && normalized.contains("30")) return new double[]{20000000, 30000000};
        if (normalized.contains("30") && normalized.contains("50")) return new double[]{30000000, 50000000};
        return new double[]{0, 0};
    }

    private void updateSortUi() {
        styleSortButton(sortPopularButton, activeSort == SORT_POPULAR);
        styleSortButton(sortPromotionButton, activeSort == SORT_PROMOTION);
        styleSortButton(sortPriceButton, activeSort == SORT_PRICE);
        if (sortPriceButton != null) {
            if (activeSort == SORT_PRICE) {
                sortPriceButton.setText(priceAscending
                        ? R.string.products_sort_price_asc
                        : R.string.products_sort_price_desc);
            } else {
                sortPriceButton.setText(R.string.products_sort_price);
            }
        }
        if (sortActiveIndicator != null) {
            sortActiveIndicator.post(() -> {
                View row = findViewById(R.id.productSortFilterRow);
                if (row == null) return;
                int rowWidth = row.getWidth();
                if (rowWidth <= 0) return;
                int slotWidth = rowWidth / 4;
                int indicatorWidth = sortActiveIndicator.getWidth();
                int targetIndex = Math.min(activeSort, 3);
                float targetX = targetIndex * slotWidth + (slotWidth - indicatorWidth) / 2f;
                sortActiveIndicator.animate().translationX(targetX).setDuration(120).start();
            });
        }
    }

    private void styleSortButton(TextView button, boolean selected) {
        if (button == null) return;
        button.setTextColor(getColor(selected ? R.color.aura_orange : R.color.aura_nav_muted));
        button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readItems(Map<String, Object> body) {
        Object value = body.get("items");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        value = body.get("products");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        value = body.get("data");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        if (value instanceof Map) {
            Object nestedItems = ((Map<?, ?>) value).get("items");
            if (nestedItems instanceof List) {
                return (List<Map<String, Object>>) nestedItems;
            }
        }
        return new ArrayList<>();
    }

    private void renderProducts(List<Map<String, Object>> products) {
        if (productGrid == null) return;
        productGrid.removeAllViews();
        if (products == null || products.isEmpty()) {
            showEmptyOrError(R.string.products_empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> product = products.get(i);
            FrameLayout wrapper = new FrameLayout(this);
            GridLayout.LayoutParams wrapperParams = new GridLayout.LayoutParams();
            wrapperParams.width = 0;
            wrapperParams.height = dpToPx(326);
            wrapperParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            wrapperParams.setMargins(
                    i % 2 == 0 ? 0 : dpToPx(5),
                    i % 2 == 0 ? 0 : dpToPx(12),
                    i % 2 == 0 ? dpToPx(5) : 0,
                    dpToPx(14)
            );
            wrapper.setLayoutParams(wrapperParams);

            View card = inflater.inflate(R.layout.item_aura_product_card, wrapper, false);
            bindProductCard(card, product);
            card.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
            wrapper.addView(card, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            productGrid.addView(wrapper);
        }
    }

    private void bindProductCard(View card, Map<String, Object> product) {
        TextView title = card.findViewById(R.id.productTitle);
        TextView price = card.findViewById(R.id.productPriceText);
        TextView oldPrice = card.findViewById(R.id.oldPriceText);
        TextView rating = card.findViewById(R.id.productRatingText);
        TextView saleBadge = card.findViewById(R.id.productSaleBadge);
        TextView specOne = card.findViewById(R.id.productSpecOneText);
        TextView specTwo = card.findViewById(R.id.productSpecTwoText);
        ImageView image = card.findViewById(R.id.productImage);

        String name = stringValue(product.get("name"), getString(R.string.home_product_case_name));
        if (title != null) {
            title.setText(name);
        }

        double priceValue = firstNumber(product, "price", "sale_price", "final_price");
        double oldPriceValue = firstNumber(product, "old_price", "original_price", "compare_at_price", "market_price");
        if (price != null) {
            price.setText(formatPrice(priceValue));
        }
        if (oldPrice != null) {
            if (oldPriceValue > 0 && oldPriceValue > priceValue) {
                oldPrice.setVisibility(View.VISIBLE);
                oldPrice.setText(formatPrice(oldPriceValue));
                oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                oldPrice.setVisibility(View.INVISIBLE);
            }
        }
        if (saleBadge != null) {
            int discount = discountPercent(priceValue, oldPriceValue);
            if (discount > 0) {
                saleBadge.setText("-" + discount + "%");
                saleBadge.setVisibility(View.VISIBLE);
            } else {
                saleBadge.setVisibility(View.GONE);
            }
        }
        if (rating != null) {
            rating.setText(getString(R.string.product_rating) + " " + getString(R.string.product_review_count));
        }

        List<String> specs = readSpecTags(product, name);
        bindSpec(specOne, specs, 0);
        bindSpec(specTwo, specs, 1);

        String imageUrl = firstImageUrl(product);
        if (image != null && !TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.figma_sale_case)
                    .error(R.drawable.figma_sale_case)
                    .into(image);
        } else if (image != null) {
            image.setImageResource(R.drawable.figma_sale_case);
        }
    }

    private void bindSpec(TextView view, List<String> specs, int index) {
        if (view == null) return;
        if (specs.size() > index) {
            view.setText(specs.get(index));
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("unchecked")
    private String firstImageUrl(Map<String, Object> product) {
        Object images = product.get("images");
        if (images instanceof List && !((List<?>) images).isEmpty()) {
            Object firstImage = ((List<?>) images).get(0);
            if (firstImage instanceof Map) {
                Object url = ((Map<String, Object>) firstImage).get("url");
                return stringValue(url, "");
            }
            return stringValue(firstImage, "");
        }
        for (String key : new String[]{"image_url", "thumbnail", "image", "photo"}) {
            String value = stringValue(product.get(key), "");
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private List<String> readSpecTags(Map<String, Object> product, String name) {
        List<String> tags = new ArrayList<>();
        String searchable = (name + " "
                + stringValue(product.get("description"), "") + " "
                + stringValue(product.get("specs"), "") + " "
                + stringValue(product.get("attributes"), "")).toUpperCase(Locale.ROOT);
        addFirstMatch(tags, searchable, new String[]{"RTX 4090", "RTX 4080", "RTX 4070", "RTX 4060", "RTX 4050", "RTX 3050", "GTX 1650"});
        addFirstMatch(tags, searchable, new String[]{"64GB RAM", "32GB RAM", "16GB RAM", "8GB RAM", "64GB", "32GB", "16GB", "8GB"});
        addFirstMatch(tags, searchable, new String[]{"2TB SSD", "1TB SSD", "512GB SSD", "256GB SSD"});
        while (tags.size() > 2) {
            tags.remove(tags.size() - 1);
        }
        return tags;
    }

    private void addFirstMatch(List<String> tags, String source, String[] candidates) {
        if (tags.size() >= 2) return;
        for (String candidate : candidates) {
            if (source.contains(candidate) && !tags.contains(candidate)) {
                tags.add(candidate);
                return;
            }
        }
    }

    private double firstNumber(Map<String, Object> product, String... keys) {
        for (String key : keys) {
            double value = numberValue(product.get(key));
            if (value > 0) return value;
        }
        return 0;
    }

    private int discountPercent(double price, double oldPrice) {
        if (price <= 0 || oldPrice <= price) return 0;
        return Math.max(1, (int) Math.round((oldPrice - price) * 100 / oldPrice));
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? fallback : text.trim();
    }

    private double numberValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value == null) return 0;
        String digits = String.valueOf(value).replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatPrice(double price) {
        if (price <= 0) {
            return getString(R.string.product_contact_price);
        }
        return currencyFormat.format(price) + "đ";
    }

    private void showEmptyOrError(int messageRes) {
        if (productGrid != null) {
            productGrid.removeAllViews();
            TextView message = new TextView(this);
            message.setGravity(Gravity.CENTER);
            message.setText(messageRes);
            message.setTextColor(getColor(R.color.aura_muted));
            message.setTextSize(14);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = dpToPx(160);
            params.columnSpec = GridLayout.spec(0, 2);
            message.setLayoutParams(params);
            productGrid.addView(message);
        }
    }

    private void setLoading(boolean loading) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private String nonEmpty(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String normalizeSearchText(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private static class CategoryItem {
        final String categoryId;
        final String parentId;
        final String name;
        final int level;

        CategoryItem(String categoryId, String parentId, String name, int level) {
            this.categoryId = categoryId;
            this.parentId = parentId;
            this.name = name;
            this.level = level;
        }

        static CategoryItem from(Map<String, Object> raw) {
            return new CategoryItem(
                    stringValue(raw.get("category_id")),
                    nullableString(raw.get("parent_id")),
                    stringValue(raw.get("name")),
                    intValue(raw.get("level"))
            );
        }

        private static String stringValue(Object value) {
            return value == null ? "" : String.valueOf(value).trim();
        }

        private static String nullableString(Object value) {
            if (value == null) return null;
            String text = String.valueOf(value).trim();
            return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
        }

        private static int intValue(Object value) {
            if (value instanceof Number) return ((Number) value).intValue();
            return 0;
        }
    }
}
