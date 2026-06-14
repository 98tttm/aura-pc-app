package com.aura.pc.ui.categories;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.ui.products.AuraProductsActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.utils.LocaleManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesActivity extends AppCompatActivity {
    private final List<CategoryItem> categories = new ArrayList<>();
    private final List<CategoryItem> rootCategories = new ArrayList<>();
    private final Map<String, List<CategoryItem>> childrenByParent = new HashMap<>();
    private final Set<String> selectedDescendantIds = new HashSet<>();
    private CategoryItem selectedRoot;
    private LinearLayout sidebarList;
    private TextView mainTitle;
    private TextView productCount;
    private GridLayout brandGrid;
    private GridLayout priceGrid;
    private GridLayout needsGrid;
    private LinearLayout seriesList;
    private LinearLayout cpuChipRow;
    private GridLayout gpuChipGrid;
    private TextView moreBrandsButton;
    private ProgressBar loadingProgress;
    private boolean showAllBrands;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        bindViews();
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);
        setupHeaderActions();
        renderStaticFilters();
        loadCategoriesFromBackend();
    }

    private void bindViews() {
        sidebarList = findViewById(R.id.categorySidebarList);
        mainTitle = findViewById(R.id.categoryMainTitle);
        productCount = findViewById(R.id.categoryProductCount);
        brandGrid = findViewById(R.id.categoryBrandGrid);
        priceGrid = findViewById(R.id.categoryPriceGrid);
        needsGrid = findViewById(R.id.categoryNeedsGrid);
        seriesList = findViewById(R.id.categorySeriesList);
        cpuChipRow = findViewById(R.id.categoryCpuChipRow);
        gpuChipGrid = findViewById(R.id.categoryGpuChipGrid);
        moreBrandsButton = findViewById(R.id.categoryMoreBrandsButton);
        loadingProgress = findViewById(R.id.categoryLoadingProgress);
    }

    private void setupHeaderActions() {
        View cart = findViewById(R.id.categoryCartButton);
        View notifications = findViewById(R.id.categoryNotificationsButton);
        View search = findViewById(R.id.categorySearchBox);
        View filter = findViewById(R.id.categoryFilterButton);
        View viewAll = findViewById(R.id.categoryViewAllButton);

        if (cart != null) {
            cart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        }
        if (notifications != null) {
            notifications.setOnClickListener(v ->
                    Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        }
        if (search != null) {
            search.setOnClickListener(v -> openProductList(selectedRoot));
        }
        if (filter != null) {
            filter.setOnClickListener(v ->
                    Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
        }
        if (viewAll != null) {
            viewAll.setOnClickListener(v -> openProductList(selectedRoot));
        }
        if (moreBrandsButton != null) {
            moreBrandsButton.setOnClickListener(v -> {
                showAllBrands = true;
                renderBrandGrid();
            });
        }
    }

    private void loadCategoriesFromBackend() {
        setLoading(true);
        ApiClient.getInstance(this).getApiService().getCategories().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    consumeCategories(response.body());
                    return;
                }
                showFallbackCategories();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CategoriesActivity.this, R.string.category_load_failed, Toast.LENGTH_SHORT).show();
                showFallbackCategories();
            }
        });
    }

    private void consumeCategories(List<Map<String, Object>> rawItems) {
        categories.clear();
        rootCategories.clear();
        childrenByParent.clear();

        for (Map<String, Object> raw : rawItems) {
            CategoryItem item = CategoryItem.from(raw);
            if (item.categoryId.isEmpty() || item.name.isEmpty()) {
                continue;
            }
            categories.add(item);
            String parentKey = item.parentId == null ? "" : item.parentId;
            List<CategoryItem> siblings = childrenByParent.get(parentKey);
            if (siblings == null) {
                siblings = new ArrayList<>();
                childrenByParent.put(parentKey, siblings);
            }
            siblings.add(item);
            if (item.parentId == null || item.parentId.isEmpty()) {
                rootCategories.add(item);
            }
        }

        sortRootCategories();
        for (List<CategoryItem> siblings : childrenByParent.values()) {
            Collections.sort(siblings, (a, b) -> a.name.compareToIgnoreCase(b.name));
        }

        selectedRoot = findById("laptop");
        if (selectedRoot == null && !rootCategories.isEmpty()) {
            selectedRoot = rootCategories.get(0);
        }
        renderAll();
    }

    private void showFallbackCategories() {
        List<Map<String, Object>> fallback = new ArrayList<>();
        fallback.add(mapCategory("laptop", null, "Laptop", 1));
        fallback.add(mapCategory("pc", null, "PC", 1));
        fallback.add(mapCategory("linh-kien", null, "Linh Kiện", 1));
        fallback.add(mapCategory("phu-kien", null, "Phụ kiện", 1));
        fallback.add(mapCategory("man-hinh", null, "Màn hình", 1));
        fallback.add(mapCategory("gaming-gear", null, "Gaming gear", 1));
        fallback.add(mapCategory("ban-ghe", null, "Bàn-Ghế", 1));
        fallback.add(mapCategory("laptop-asus", "laptop", "ASUS", 2));
        fallback.add(mapCategory("laptop-lenovo", "laptop", "LENOVO", 2));
        fallback.add(mapCategory("laptop-gaming-ai", "laptop", "Laptop A.I", 2));
        fallback.add(mapCategory("laptop-van-phong", "laptop", "Văn phòng", 2));
        fallback.add(mapCategory("laptop-sinh-vien", "laptop", "SINH VIÊN", 2));
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

    private void renderAll() {
        if (selectedRoot == null) return;
        collectSelectedDescendants();
        renderSidebar();
        renderContent();
        loadProductCount(selectedRoot.categoryId);
    }

    private void renderSidebar() {
        if (sidebarList == null) return;
        sidebarList.removeAllViews();
        for (CategoryItem item : rootCategories) {
            sidebarList.addView(createSidebarItem(item));
        }
    }

    private View createSidebarItem(CategoryItem item) {
        FrameLayout container = new FrameLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(78)
        ));
        container.setBackgroundResource(isSelected(item) ? R.drawable.bg_category_sidebar_active : android.R.color.transparent);
        container.setClickable(true);
        container.setFocusable(true);
        container.setOnClickListener(v -> {
            selectedRoot = item;
            showAllBrands = false;
            renderAll();
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        container.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageResource(iconForCategory(item.categoryId));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(56), dpToPx(42));
        content.addView(icon, iconParams);

        TextView label = new TextView(this);
        label.setGravity(Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setSingleLine(false);
        label.setMaxLines(2);
        label.setText(item.name);
        label.setTextColor(isSelected(item) ? getColor(R.color.aura_orange) : getColor(R.color.aura_muted));
        label.setTextSize(10);
        label.setTypeface(Typeface.DEFAULT, isSelected(item) ? Typeface.BOLD : Typeface.NORMAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dpToPx(2);
        content.addView(label, labelParams);

        if (isSelected(item)) {
            View indicator = new View(this);
            indicator.setBackgroundResource(R.drawable.bg_category_sidebar_indicator);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dpToPx(4), dpToPx(48));
            params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
            container.addView(indicator, params);
        }
        return container;
    }

    private void renderContent() {
        if (mainTitle != null) mainTitle.setText(selectedRoot.name);
        renderBrandGrid();
        renderNeeds();
        renderSeries();
        renderChipRows();
    }

    private void renderBrandGrid() {
        if (brandGrid == null) return;
        brandGrid.removeAllViews();
        List<CategoryItem> directChildren = childrenOf(selectedRoot.categoryId);
        List<CategoryItem> brands = new ArrayList<>();
        for (CategoryItem child : directChildren) {
            if (!showAllBrands && brands.size() >= 6) break;
            brands.add(child);
        }
        if (brands.isEmpty()) {
            brands.add(selectedRoot);
        }
        for (CategoryItem brand : brands) {
            brandGrid.addView(createSmallButton(brand.name, false, () -> openProductList(brand)));
        }
        int hiddenCount = Math.max(0, directChildren.size() - brands.size());
        if (moreBrandsButton != null) {
            moreBrandsButton.setVisibility(hiddenCount > 0 ? View.VISIBLE : View.GONE);
            if (hiddenCount > 0) {
                moreBrandsButton.setText(getString(R.string.category_more_brands_dynamic, hiddenCount));
            }
        }
    }

    private void renderStaticFilters() {
        if (priceGrid != null) {
            priceGrid.removeAllViews();
            for (String label : Arrays.asList("Dưới 10tr", "10 - 15tr", "15 - 20tr", "20 - 30tr", "30 - 50tr", "Trên 50tr")) {
                priceGrid.addView(createSmallButton(label, false,
                        () -> openProductList(selectedRoot, AuraProductsActivity.FILTER_TYPE_PRICE, label, 0, 0)));
            }
        }
    }

    private void renderNeeds() {
        if (needsGrid == null) return;
        needsGrid.removeAllViews();
        List<CategoryItem> candidates = childrenOf(selectedRoot.categoryId);
        List<CategoryItem> needs = filterByKeywords(candidates, Arrays.asList("văn phòng", "sinh viên", "gaming", "ai", "đồ họa"));
        List<String> fallbackLabels = Arrays.asList("Văn phòng", "Sinh viên", "Gaming");
        int fallbackIndex = 0;
        while (needs.size() < 3 && fallbackIndex < fallbackLabels.size()) {
            String label = fallbackLabels.get(fallbackIndex);
            if (!containsLabel(needs, label)) {
                CategoryItem fallback = new CategoryItem(selectedRoot.categoryId, selectedRoot.parentId, label, selectedRoot.level);
                needs.add(fallback);
            }
            fallbackIndex++;
        }
        for (int i = 0; i < Math.min(3, needs.size()); i++) {
            CategoryItem item = needs.get(i);
            needsGrid.addView(createNeedCard(item.name, item));
        }
    }

    private void renderSeries() {
        if (seriesList == null) return;
        seriesList.removeAllViews();
        List<CategoryItem> directChildren = childrenOf(selectedRoot.categoryId);
        List<CategoryItem> series = new ArrayList<>();
        for (CategoryItem child : directChildren) {
            List<CategoryItem> descendants = childrenOf(child.categoryId);
            if (!descendants.isEmpty()) {
                series.add(child);
            }
            if (series.size() >= 2) break;
        }
        if (series.isEmpty()) {
            for (int i = 0; i < Math.min(2, directChildren.size()); i++) {
                series.add(directChildren.get(i));
            }
        }
        for (CategoryItem item : series) {
            seriesList.addView(createSeriesRow(item));
        }
    }

    private void renderChipRows() {
        if (cpuChipRow != null) {
            cpuChipRow.removeAllViews();
            for (String label : Arrays.asList("Intel Core i9", "Intel Ultra 7", "Ryzen AI 9", "Apple M3 Pro")) {
                cpuChipRow.addView(createPillChip(label, false,
                        () -> openProductList(selectedRoot, AuraProductsActivity.FILTER_TYPE_FEATURE, label, 0, 0)));
            }
        }
        if (gpuChipGrid != null) {
            gpuChipGrid.removeAllViews();
            for (String label : Arrays.asList("RTX 4090", "RTX 4080", "RTX 4070 Ti", "RX 7900 XTX")) {
                gpuChipGrid.addView(createSmallButton(label, true,
                        () -> openProductList(selectedRoot, AuraProductsActivity.FILTER_TYPE_FEATURE, label, 0, 0)));
            }
        }
    }

    private TextView createSmallButton(String text, boolean soft, Runnable action) {
        TextView button = new TextView(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(34);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        button.setLayoutParams(params);
        button.setBackgroundResource(soft ? R.drawable.bg_category_content_card_soft : R.drawable.bg_category_content_card);
        button.setClickable(true);
        button.setFocusable(true);
        button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setMaxLines(1);
        button.setPadding(dpToPx(5), 0, dpToPx(5), 0);
        button.setText(text);
        button.setTextColor(getColor(R.color.aura_ink));
        button.setTextSize(10);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private View createNeedCard(String label, CategoryItem target) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_category_content_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));
        card.setOnClickListener(v -> openProductList(target, AuraProductsActivity.FILTER_TYPE_FEATURE, label, 0, 0));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(78);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        card.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconForCategory(target.categoryId));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(dpToPx(48), dpToPx(39)));

        TextView text = new TextView(this);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.setGravity(Gravity.CENTER);
        text.setIncludeFontPadding(false);
        text.setMaxLines(1);
        text.setText(label);
        text.setTextColor(getColor(R.color.aura_ink));
        text.setTextSize(9);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(text, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return card;
    }

    private View createSeriesRow(CategoryItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_category_content_card);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dpToPx(11), dpToPx(8), dpToPx(9), dpToPx(8));
        row.setOnClickListener(v -> openProductList(item));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(68)
        );
        rowParams.setMargins(0, 0, 0, dpToPx(7));
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        icon.setBackgroundResource(R.drawable.bg_category_icon_soft);
        icon.setImageResource(iconForCategory(item.categoryId));
        icon.setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(icon, new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelsParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelsParams.setMarginStart(dpToPx(14));
        row.addView(labels, labelsParams);

        TextView title = new TextView(this);
        title.setIncludeFontPadding(false);
        title.setText(item.name);
        title.setTextColor(getColor(R.color.aura_ink));
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labels.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setIncludeFontPadding(false);
        subtitle.setText(subtitleForSeries(item));
        subtitle.setTextColor(getColor(R.color.aura_nav_muted));
        subtitle.setTextSize(10);
        labels.addView(subtitle);

        TextView arrow = new TextView(this);
        arrow.setGravity(Gravity.CENTER);
        arrow.setText(R.string.home_arrow_next);
        arrow.setTextColor(getColor(R.color.aura_muted));
        arrow.setTextSize(24);
        row.addView(arrow, new LinearLayout.LayoutParams(dpToPx(20), ViewGroup.LayoutParams.MATCH_PARENT));
        return row;
    }

    private TextView createPillChip(String label, boolean active, Runnable action) {
        TextView chip = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(34)
        );
        params.setMarginEnd(dpToPx(8));
        chip.setLayoutParams(params);
        chip.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_category_content_card);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setGravity(Gravity.CENTER);
        chip.setIncludeFontPadding(false);
        chip.setMinWidth(dpToPx(88));
        chip.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        chip.setText(label);
        chip.setTextColor(active ? Color.WHITE : getColor(R.color.aura_ink));
        chip.setTextSize(11);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setOnClickListener(v -> action.run());
        return chip;
    }

    private void loadProductCount(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) return;
        ApiClient.getInstance(this).getApiService()
                .getProductsByCategory(categoryId, 1, 1)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Object total = response.body().get("total");
                            if (total instanceof Number && productCount != null) {
                                productCount.setText(getString(R.string.category_products_count, ((Number) total).intValue()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        if (productCount != null) productCount.setText("");
                    }
                });
    }

    private void openProductList(CategoryItem item) {
        openProductList(item, "", "", 0, 0);
    }

    private void openProductList(CategoryItem item, String filterType, String filterLabel, double minPrice, double maxPrice) {
        if (item == null) return;
        Intent intent = new Intent(this, AuraProductsActivity.class);
        intent.putExtra(AuraProductsActivity.EXTRA_CATEGORY_ID, item.categoryId);
        intent.putExtra(AuraProductsActivity.EXTRA_CATEGORY_NAME, item.name);
        intent.putExtra(AuraProductsActivity.EXTRA_SELECTED_FILTER_TYPE, filterType);
        intent.putExtra(AuraProductsActivity.EXTRA_SELECTED_FILTER_LABEL, filterLabel);
        intent.putExtra(AuraProductsActivity.EXTRA_PRICE_MIN, minPrice);
        intent.putExtra(AuraProductsActivity.EXTRA_PRICE_MAX, maxPrice);
        startActivity(intent);
    }

    private List<CategoryItem> filterByKeywords(List<CategoryItem> source, List<String> keywords) {
        List<CategoryItem> result = new ArrayList<>();
        for (CategoryItem item : source) {
            String name = item.name.toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (name.contains(keyword)) {
                    result.add(item);
                    break;
                }
            }
        }
        return result;
    }

    private boolean containsLabel(List<CategoryItem> items, String label) {
        String normalizedLabel = label.toLowerCase(Locale.ROOT);
        for (CategoryItem item : items) {
            if (item.name.toLowerCase(Locale.ROOT).contains(normalizedLabel)) {
                return true;
            }
        }
        return false;
    }

    private List<CategoryItem> childrenOf(String parentId) {
        List<CategoryItem> children = childrenByParent.get(parentId);
        return children == null ? new ArrayList<>() : children;
    }

    private void collectSelectedDescendants() {
        selectedDescendantIds.clear();
        if (selectedRoot != null) {
            collectDescendants(selectedRoot.categoryId);
        }
    }

    private void collectDescendants(String parentId) {
        for (CategoryItem child : childrenOf(parentId)) {
            selectedDescendantIds.add(child.categoryId);
            collectDescendants(child.categoryId);
        }
    }

    private CategoryItem findById(String categoryId) {
        for (CategoryItem item : categories) {
            if (item.categoryId.equals(categoryId)) return item;
        }
        return null;
    }

    private boolean isSelected(CategoryItem item) {
        return selectedRoot != null && selectedRoot.categoryId.equals(item.categoryId);
    }

    private void sortRootCategories() {
        List<String> order = Arrays.asList("laptop", "pc", "linh-kien", "phu-kien", "man-hinh", "gaming-gear", "ban-ghe");
        Collections.sort(rootCategories, (a, b) -> Integer.compare(indexOf(order, a.categoryId), indexOf(order, b.categoryId)));
    }

    private int indexOf(List<String> order, String id) {
        int index = order.indexOf(id);
        return index >= 0 ? index : 999;
    }

    private String subtitleForSeries(CategoryItem item) {
        int childCount = childrenOf(item.categoryId).size();
        if (childCount > 0) {
            return getString(R.string.category_products_count, childCount);
        }
        return getString(R.string.category_item_cta);
    }

    private int iconForCategory(String categoryId) {
        if (categoryId == null) return R.drawable.ic_grid;
        if (categoryId.equals("linh-kien")) return R.drawable.figma_cat_component_ryzen;
        if (categoryId.equals("ban-ghe")) return R.drawable.figma_cat_component;
        if (categoryId.equals("phu-kien")) return R.drawable.figma_cat_accessory;
        if (categoryId.contains("laptop")) return R.drawable.figma_cat_laptop;
        if (categoryId.contains("pc")) return R.drawable.figma_cat_gaming_pc;
        if (categoryId.contains("linh-kien")) return R.drawable.figma_cat_component_ryzen;
        if (categoryId.contains("cpu")) return R.drawable.figma_component_cpu;
        if (categoryId.contains("man-hinh")) return R.drawable.figma_cat_monitor;
        if (categoryId.contains("gaming-gear") || categoryId.contains("ban-phim") || categoryId.contains("chuot")) {
            return R.drawable.figma_cat_keyboard_figma;
        }
        if (categoryId.contains("ban-ghe") || categoryId.contains("ghe") || categoryId.contains("ban-")) {
            return R.drawable.figma_cat_component;
        }
        if (categoryId.contains("phu-kien")) return R.drawable.figma_cat_accessory;
        if (categoryId.contains("vga")) return R.drawable.figma_component_vga;
        if (categoryId.contains("ram")) return R.drawable.figma_component_ram;
        if (categoryId.contains("hdd")) return R.drawable.figma_component_hdd;
        if (categoryId.contains("ssd")) return R.drawable.figma_component_ssd;
        if (categoryId.contains("mainboard")) return R.drawable.figma_component_mainboard;
        if (categoryId.contains("nguon") || categoryId.contains("psu")) return R.drawable.figma_component_psu;
        if (categoryId.contains("quat") || categoryId.contains("fan")) return R.drawable.figma_component_fan;
        if (categoryId.contains("tan-nhiet") || categoryId.contains("cooler")) return R.drawable.figma_component_cooler;
        return R.drawable.ic_grid;
    }

    private void setLoading(boolean loading) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
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
            return value == null ? "" : String.valueOf(value);
        }

        private static String nullableString(Object value) {
            if (value == null) return null;
            String text = String.valueOf(value);
            return text.trim().isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
        }

        private static int intValue(Object value) {
            if (value instanceof Number) return ((Number) value).intValue();
            return 0;
        }
    }
}
