package com.example.aura_pc_app.ui.home;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.ui.categories.CategoriesActivity;
import com.aura.pc.ui.productdetail.ProductDetailActivity;
import com.aura.pc.ui.products.AuraProductsActivity;
import com.aura.pc.ui.products.ProductSearchActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.adapter.HomeProductAdapter;
import com.example.aura_pc_app.adapter.HomeSaleProductAdapter;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.cart.CartRepositoryImpl;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.databinding.ActivityHomeBinding;
import com.example.aura_pc_app.domain.cart.CartRepository;
import com.example.aura_pc_app.ui.base.BaseActivity;
import com.example.aura_pc_app.utils.AuthGate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends BaseActivity<ActivityHomeBinding> {
    private HomeProductAdapter productAdapter;
    private HomeSaleProductAdapter saleProductAdapter;
    private boolean collapsedHeaderVisible;
    private String selectedProductCategoryId = "laptop";
    private int selectedSaleCampaignIndex;
    private int selectedSaleDateIndex;
    private final Map<String, List<CategoryChip>> childCategoriesByParent = new HashMap<>();
    private final List<ProductEntity> allSaleProducts = new ArrayList<>();
    private CartRepository cartRepository;
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
        cartRepository = new CartRepositoryImpl(this);
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_HOME);
        setupSaleSection();
        setupProductList();
        setupHomeActions();
        observeCartBadge();
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
        binding.homeSearchContainer.setOnClickListener(v -> openProductSearch());
        binding.homeProductSearchView.setOnClickListener(v -> openProductSearch());
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

    private void observeCartBadge() {
        AppDatabase.getInstance(this)
                .cartDao()
                .getCartItemCountLive()
                .observe(this, count -> updateCartBadge(count == null ? 0 : count));
    }

    private void updateCartBadge(int count) {
        updateCartBadgeView(binding.topCartBadge, count);
        updateCartBadgeView(binding.stickyCartBadge, count);
    }

    private void updateCartBadgeView(TextView badge, int count) {
        if (badge == null) {
            return;
        }
        if (count <= 0) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
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

    private void setupSaleSection() {
        saleProductAdapter = new HomeSaleProductAdapter(this::openProductDetail);
        binding.homeSaleRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.homeSaleRecyclerView.setAdapter(saleProductAdapter);
        binding.homeSaleRecyclerView.setHasFixedSize(false);
        binding.homeSaleRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
                updateRecyclerIndicator(
                        binding.homeSaleRecyclerView,
                        binding.homeSaleIndicatorTrack,
                        binding.homeSaleIndicatorThumb);
            }
        });

        binding.homeSaleTabFlash.setOnClickListener(v -> selectSaleCampaign(0));
        binding.homeSaleTabDeal.setOnClickListener(v -> selectSaleCampaign(1));
        binding.homeSaleTabHot.setOnClickListener(v -> selectSaleCampaign(2));
        binding.homeSaleDateOne.setOnClickListener(v -> selectSaleDate(0));
        binding.homeSaleDateTwo.setOnClickListener(v -> selectSaleDate(1));
        binding.homeSaleDateThree.setOnClickListener(v -> selectSaleDate(2));
        updateSaleDateLabels();
        updateSaleSelectionUi();
        saleCountdownHandler.removeCallbacks(saleCountdownRunnable);
        saleCountdownHandler.post(saleCountdownRunnable);
    }

    private void selectSaleCampaign(int campaignIndex) {
        selectedSaleCampaignIndex = campaignIndex;
        updateSaleSelectionUi();
        updateSaleCountdown();
        renderSaleProducts();
    }

    private void selectSaleDate(int dateIndex) {
        selectedSaleDateIndex = dateIndex;
        updateSaleSelectionUi();
        updateSaleCountdown();
        renderSaleProducts();
    }

    private void updateSaleSelectionUi() {
        updateSaleTab(binding.homeSaleTabFlash, selectedSaleCampaignIndex == 0);
        updateSaleTab(binding.homeSaleTabDeal, selectedSaleCampaignIndex == 1);
        updateSaleTab(binding.homeSaleTabHot, selectedSaleCampaignIndex == 2);
        updateSaleDateTab(binding.homeSaleDateOne, selectedSaleDateIndex == 0);
        updateSaleDateTab(binding.homeSaleDateTwo, selectedSaleDateIndex == 1);
        updateSaleDateTab(binding.homeSaleDateThree, selectedSaleDateIndex == 2);
    }

    private void updateSaleTab(TextView tab, boolean selected) {
        tab.setAlpha(1f);
        tab.setScaleX(1f);
        tab.setScaleY(1f);
    }

    private void updateSaleDateTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_home_date_selected : R.drawable.bg_home_date_outline);
        tab.setTextColor(selected ? 0xFFFF1F1F : getColor(R.color.aura_white));
    }

    private void updateSaleDateLabels() {
        TextView[] dateTabs = {
                binding.homeSaleDateOne,
                binding.homeSaleDateTwo,
                binding.homeSaleDateThree
        };
        Calendar date = Calendar.getInstance();
        for (int i = 0; i < dateTabs.length; i++) {
            if (i > 0) {
                date.add(Calendar.DAY_OF_YEAR, 1);
            }
            dateTabs[i].setText(String.format(Locale.US, "%02d/%02d",
                    date.get(Calendar.DAY_OF_MONTH),
                    date.get(Calendar.MONTH) + 1));
        }
    }

    private void updateSaleCountdown() {
        Calendar now = Calendar.getInstance();
        SaleCountdownTarget target = saleCountdownTarget(now);
        long remainingMs = Math.max(0, target.targetTime.getTimeInMillis() - now.getTimeInMillis());
        long totalSeconds = remainingMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        binding.homeSaleCountdownLabel.setText(target.labelResId);
        binding.homeSaleCountdownHour.setText(String.format(Locale.US, "%02d", hours));
        binding.homeSaleCountdownMinute.setText(String.format(Locale.US, "%02d", minutes));
        binding.homeSaleCountdownSecond.setText(String.format(Locale.US, "%02d", seconds));
    }

    private SaleCountdownTarget saleCountdownTarget(Calendar now) {
        Calendar selectedDate = saleSelectedDate();
        if (isSameDay(now, selectedDate)) {
            Calendar endOfDay = (Calendar) selectedDate.clone();
            endOfDay.set(Calendar.HOUR_OF_DAY, 23);
            endOfDay.set(Calendar.MINUTE, 59);
            endOfDay.set(Calendar.SECOND, 59);
            endOfDay.set(Calendar.MILLISECOND, 999);
            return new SaleCountdownTarget(endOfDay, R.string.home_flash_countdown_remaining_label);
        }

        int[] starts = {9, 13, 20};
        int startHour = starts[Math.max(0, Math.min(selectedSaleCampaignIndex, starts.length - 1))];
        Calendar startTime = (Calendar) selectedDate.clone();
        startTime.set(Calendar.HOUR_OF_DAY, startHour);
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.SECOND, 0);
        startTime.set(Calendar.MILLISECOND, 0);

        if (now.before(startTime)) {
            return new SaleCountdownTarget(startTime, R.string.home_flash_countdown_label);
        }

        Calendar nextStart = (Calendar) startTime.clone();
        nextStart.add(Calendar.DAY_OF_YEAR, 1);
        return new SaleCountdownTarget(nextStart, R.string.home_flash_countdown_label);
    }

    private Calendar saleSelectedDate() {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.add(Calendar.DAY_OF_YEAR, selectedSaleDateIndex);
        return selectedDate;
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private void setupHomeProductTabs() {
        binding.homeProductTabLaptop.setOnClickListener(v -> selectHomeProductCategory("laptop"));
        binding.homeProductTabPc.setOnClickListener(v -> selectHomeProductCategory("pc"));
        binding.homeProductTabMonitor.setOnClickListener(v -> selectHomeProductCategory("man-hinh"));
        binding.homeProductTabAccessory.setOnClickListener(v -> selectHomeProductCategory("phu-kien"));
        updateHomeProductTabs();
    }

    private void selectHomeProductCategory(String categoryId) {
        selectedProductCategoryId = categoryId;
        updateHomeProductTabs();
        renderHomeBrandChips();
        binding.productRecyclerView.scrollToPosition(0);
        loadHomeProducts(categoryId);
    }

    private void updateHomeProductTabs() {
        updateHomeProductTab(binding.homeProductTabLaptop, "laptop");
        updateHomeProductTab(binding.homeProductTabPc, "pc");
        updateHomeProductTab(binding.homeProductTabMonitor, "man-hinh");
        updateHomeProductTab(binding.homeProductTabAccessory, "phu-kien");
    }

    private void updateHomeProductTab(TextView tab, String categoryId) {
        boolean selected = categoryId.equals(selectedProductCategoryId);
        tab.setBackgroundResource(selected ? R.drawable.bg_home_top_tab_active : R.drawable.bg_home_top_tab);
        tab.setTypeface(tab.getTypeface(), selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void loadHomeCategories() {
        ApiClient.getInstance(this).getApiService().getCategories().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    consumeHomeCategories(response.body());
                }
                renderHomeBrandChips();
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                renderHomeBrandChips();
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
                product._id = stringValue(raw.get("id"));
            }
            if (product._id.isEmpty()) {
                product._id = stringValue(raw.get("productId"));
            }
            if (product._id.isEmpty()) {
                product._id = stringValue(raw.get("product_id"));
            }
            product.name = stringValue(raw.get("name"));
            product.slug = stringValue(raw.get("slug"));
            product.price = numberValue(firstNonNull(raw, "price", "sale_price", "salePrice", "final_price"));
            product.salePrice = nullableNumber(firstNonNull(raw, "salePrice", "sale_price", "final_price"));
            product.oldPrice = nullableNumber(firstNonNull(raw, "old_price", "original_price", "compare_at_price", "market_price"));
            product.category_id = stringValue(raw.get("category_id"));
            product.brand = stringValue(raw.get("brand"));
            product.imageUrl = firstImageUrl(raw.get("images"));
            product.images = product.imageUrl;
            product.active = true;
            products.add(product);
        }
        return products;
    }

    private Object firstNonNull(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
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

    private void setupCategoryStrip() {
        int[] titles = {
                R.string.category_laptop,
                R.string.home_category_pc,
                R.string.category_monitor,
                R.string.home_category_gaming_gear,
                R.string.home_category_chair,
                R.string.home_category_components,
                R.string.home_category_accessory
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
            item.setOnClickListener(v -> startActivity(new Intent(this, CategoriesActivity.class)));
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

    private void openProductDetail(ProductEntity product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        if (product != null && product._id != null && !product._id.trim().isEmpty() && !product._id.startsWith("fallback-")) {
            intent.putExtra("product_id", product._id);
        }
        startActivity(intent);
    }

    private void openCategoryProducts(String categoryId, String categoryName) {
        Intent intent = new Intent(this, AuraProductsActivity.class);
        intent.putExtra(AuraProductsActivity.EXTRA_CATEGORY_ID, categoryId);
        intent.putExtra(AuraProductsActivity.EXTRA_CATEGORY_NAME, categoryName);
        startActivity(intent);
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private void openProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.putExtra(ProductSearchActivity.EXTRA_SOURCE, ProductSearchActivity.SOURCE_HOME);
        startActivity(intent);
    }

    private void addProductToCart(ProductEntity product) {
        if (!AuthGate.requireLogin(this, CartActivity.class)) {
            return;
        }
        if (product == null || product._id == null || product._id.trim().isEmpty() || product._id.startsWith("fallback-")) {
            Toast.makeText(this, "Không thể thêm sản phẩm này vào giỏ hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        cartRepository.addProduct(product, 1, new CartRepository.CartCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(HomeActivity.this, getString(R.string.toast_added_to_cart), Toast.LENGTH_SHORT).show();
                BottomNavigationHelper.setupHeader(HomeActivity.this);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<ProductEntity> createFallbackProducts() {
        List<ProductEntity> products = new ArrayList<>();
        addFallback(products, "fallback-case-one", getString(R.string.home_product_case_name), 5490000, 3990000);
        addFallback(products, "fallback-cpu-one", getString(R.string.home_product_cpu_name), 5490000, 1110000);
        addFallback(products, "fallback-case-two", getString(R.string.home_product_case_name), 5490000, 3990000);
        addFallback(products, "fallback-cpu-two", getString(R.string.home_product_cpu_name), 5490000, 1110000);
        return products;
    }

    private List<ProductEntity> createSaleFallbackProducts() {
        List<ProductEntity> products = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            if (i % 2 == 0) {
                addFallback(products, "fallback-sale-case-" + i, getString(R.string.home_product_case_name), 5490000, 3990000);
            } else {
                addFallback(products, "fallback-sale-cpu-" + i, getString(R.string.home_product_cpu_name), 5490000, 1110000);
            }
        }
        return products;
    }

    private void addFallback(List<ProductEntity> products, String id, String name, double price, double salePrice) {
        ProductEntity product = new ProductEntity();
        product._id = id;
        product.name = name;
        product.price = price;
        product.salePrice = salePrice;
        product.active = true;
        products.add(product);
    }

    private static class CategoryChip {
        final String categoryId;
        final String name;

        CategoryChip(String categoryId, String name) {
            this.categoryId = categoryId;
            this.name = name;
        }
    }

    private static class SaleCountdownTarget {
        final Calendar targetTime;
        final int labelResId;

        SaleCountdownTarget(Calendar targetTime, int labelResId) {
            this.targetTime = targetTime;
            this.labelResId = labelResId;
        }
    }
}
