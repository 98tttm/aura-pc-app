package com.aura.pc.ui.products;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.aura.pc.ui.productdetail.ProductDetailActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.databinding.ActivityProductListBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;
import com.example.aura_pc_app.utils.ProductSearchUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;
import java.util.List;

/**
 * Màn hình danh sách sản phẩm với manual Paging, Filter (BottomSheet), Sort (Spinner),
 * và Category Chips chuyển đổi nhanh.
 */
public class ProductListActivity extends BaseActivity<ActivityProductListBinding> {

    private static final int REQUEST_SEARCH = 1001;

    private ProductListViewModel viewModel;
    private ProductPagingAdapter adapter;
    private boolean isSpinnerInitialized = false;

    // Sort options
    private static final String[] SORT_LABELS = {
            "Mới nhất", "Giá tăng dần", "Giá giảm dần", "Bán chạy", "Đánh giá cao"
    };
    private static final String[] SORT_VALUES = {
            "newest", "price_asc", "price_desc", "best_selling", "top_rated"
    };

    // The currently active category chip slug to prevent loops
    private String activeChipSlug = null;
    
    private boolean isProgrammaticChange = false;

    @Override
    protected ActivityProductListBinding inflateBinding() {
        return ActivityProductListBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding.btnBack.setOnClickListener(v -> finish());
        getWindow().setStatusBarColor(getColor(R.color.aura_orange));
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);

        initViewModel();
        initRecyclerView();
        initSortSpinner();
        initCategoryChips();
        initSearchInput();
        initActions();

        // Gắn observer một lần duy nhất, trước khi kích hoạt lần tải đầu tiên.
        observeData();

        // Ưu tiên tìm theo keyword (mở từ thanh "Bạn muốn mua gì hôm nay"),
        // sau đó tới lọc theo danh mục (mở từ chip danh mục / trang Danh mục).
        String initialQuery = getIntent().getStringExtra("query");
        String initialCategory = getIntent().getStringExtra("category");
        String categoryName = getIntent().getStringExtra("categoryName");
        
        if (initialCategory != null) {
            viewModel.setParentCategorySlug(initialCategory);
            viewModel.setCategory(initialCategory);
            activeChipSlug = initialCategory;
        }
        
        if (categoryName != null && !categoryName.isEmpty()) {
            binding.categoryTitle.setText(categoryName);
        } else if (initialCategory != null) {
            // Fallback to titlecase slug
            String title = initialCategory.replace("-", " ");
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
            binding.categoryTitle.setText(title);
        } else {
            binding.categoryTitle.setText("Sản phẩm");
        }

        viewModel.fetchSubCategories();
        viewModel.loadFirstPage();
        observeData();
    }

    // ===== ViewModel =====
    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(ProductListViewModel.class);
        viewModel.init(ApiClient.getInstance(this).getApiService());
    }

    // ===== RecyclerView =====
    private void initRecyclerView() {
        RecyclerView rv = binding.rvProductGrid;
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        rv.setLayoutManager(layoutManager);

        adapter = new ProductPagingAdapter();
        adapter.setOnProductClickListener(this::openProductDetail);
        adapter.setOnAddToCartListener(product ->
                Toast.makeText(this, getString(R.string.toast_added_to_cart), Toast.LENGTH_SHORT).show()
        );
        adapter.loadFavoriteIds(this);
        rv.setAdapter(adapter);

        // Detect scroll to bottom → load next page (infinite scroll)
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return; // Chỉ kiểm tra khi cuộn xuống

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                // Khi gần cuối danh sách (còn 5 item) → tải thêm
                if (lastVisibleItem >= totalItemCount - 5) {
                    viewModel.loadNextPage();
                }
            }
        });
    }

    // ===== Observe LiveData =====
    private void observeData() {
        // Danh sách sản phẩm
        viewModel.getProductList().observe(this, products -> {
            adapter.submitList(products);
        });

        // Loading lần đầu
        viewModel.getLoading().observe(this, isLoading -> {
            binding.progressBarCenter.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                binding.rvProductGrid.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
            }
        });

        // Loading thêm (infinite scroll — hiện footer)
        viewModel.getLoadingMore().observe(this, isLoadingMore -> {
            // Có thể thêm footer loading ở đây nếu cần
        });

        // Empty state
        viewModel.getIsEmpty().observe(this, isEmpty -> {
            if (isEmpty && Boolean.FALSE.equals(viewModel.getLoading().getValue())) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.rvProductGrid.setVisibility(View.GONE);
            } else {
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.rvProductGrid.setVisibility(View.VISIBLE);
            }
        });

        // Error
        viewModel.getError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        // Dynamic Subcategories
        viewModel.getResolvedParentCategory().observe(this, parentSlug -> {
            // We use parentSlug for "Tất cả" chip
            if (parentSlug != null && viewModel.getSubCategories().getValue() != null) {
                populateCategoryChips(parentSlug, viewModel.getSubCategories().getValue());
            }
        });
        
        // Update Title to match parent category name
        viewModel.getResolvedParentName().observe(this, parentName -> {
            if (parentName != null && !parentName.isEmpty()) {
                binding.categoryTitle.setText(parentName);
            }
        });
        
        viewModel.getSubCategories().observe(this, subCategories -> {
            String parentSlug = viewModel.getResolvedParentCategory().getValue();
            if (parentSlug != null) {
                populateCategoryChips(parentSlug, subCategories);
            }
        });
    }

    // ===== Sort Spinner =====
    private void initSortSpinner() {
        Spinner spinner = binding.spinnerSort;
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, SORT_LABELS
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Bỏ qua lần gọi đầu tiên khi Spinner vừa khởi tạo
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true;
                    return;
                }
                viewModel.setSort(SORT_VALUES[position]);
                viewModel.applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ===== Category Chips =====
    private void initCategoryChips() {
        // Handled dynamically via observer
    }

    private void initSearchInput() {
        binding.searchInput.setOnClickListener(v -> openProductSearch());
    }

    private void openProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.putExtra(ProductSearchActivity.EXTRA_SOURCE, ProductSearchActivity.SOURCE_PRODUCT_LIST);
        startActivityForResult(intent, REQUEST_SEARCH);
    }

    private void populateCategoryChips(String resolvedParentSlug, List<Map<String, Object>> subCategories) {
        ChipGroup chipGroup = binding.chipGroupCategory;
        chipGroup.removeAllViews();
        chipGroup.setOnCheckedStateChangeListener(null);

        // Add "Tất cả" chip
        com.google.android.material.chip.Chip chipAll = new com.google.android.material.chip.Chip(this);
        chipAll.setText("Tất cả");
        chipAll.setCheckable(true);
        chipAll.setCheckedIconVisible(false);
        chipAll.setChipBackgroundColorResource(R.color.chip_category_bg);
        chipAll.setTextColor(getColor(R.color.chip_category_text));
        chipAll.setChipStrokeColorResource(R.color.aura_orange);
        chipAll.setChipStrokeWidth(getResources().getDisplayMetrics().density * 1);
        
        // Tag for "Tất cả" is the resolved parent category
        chipAll.setTag(resolvedParentSlug); 
        
        chipGroup.addView(chipAll);

        if (resolvedParentSlug != null && resolvedParentSlug.equals(activeChipSlug)) {
            chipAll.setChecked(true);
        }

        // Add subcategory chips
        if (subCategories != null) {
            for (Map<String, Object> cat : subCategories) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
                chip.setText((String) cat.get("name"));
                chip.setCheckable(true);
                chip.setCheckedIconVisible(false);
                chip.setChipBackgroundColorResource(R.color.chip_category_bg);
                chip.setTextColor(getColor(R.color.chip_category_text));
                chip.setChipStrokeColorResource(R.color.aura_orange);
                chip.setChipStrokeWidth(getResources().getDisplayMetrics().density * 1);
                
                String slug = (String) cat.get("slug");
                chip.setTag(slug);
                chipGroup.addView(chip);

                if (slug != null && slug.equals(activeChipSlug)) {
                    chip.setChecked(true);
                }
            }
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isProgrammaticChange) return;

            if (checkedIds.isEmpty()) {
                isProgrammaticChange = true;
                chipAll.setChecked(true);
                isProgrammaticChange = false;
                
                activeChipSlug = resolvedParentSlug;
                viewModel.setCategory(resolvedParentSlug);
                viewModel.applyFilters();
                return;
            }

            int checkedId = checkedIds.get(0);
            com.google.android.material.chip.Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null && checkedChip.getTag() != null) {
                String slug = checkedChip.getTag().toString();
                activeChipSlug = slug;
                viewModel.setCategory(slug);
                viewModel.applyFilters();
            }
        });
    }

    // ===== Action Buttons =====
    private void initActions() {
        binding.topCartButton.setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));

        binding.topNotificationsButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());

        binding.searchFilterButton.setOnClickListener(v -> showFilterDialog());
        binding.contextualFab.setOnClickListener(v -> showFilterDialog());
    }

    // ===== Filter Bottom Sheet =====
    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_product_filter, null);
        dialog.setContentView(view);

        TextInputEditText etBrand = view.findViewById(R.id.etBrand);
        TextInputEditText etMinPrice = view.findViewById(R.id.etMinPrice);
        TextInputEditText etMaxPrice = view.findViewById(R.id.etMaxPrice);
        Slider sliderRating = view.findViewById(R.id.sliderRating);
        SwitchMaterial switchInStock = view.findViewById(R.id.switchInStock);
        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilter);
        MaterialButton btnReset = view.findViewById(R.id.btnResetFilter);

        // Pre-fill
        if (viewModel.getBrand() != null) etBrand.setText(viewModel.getBrand());
        if (viewModel.getMinPrice() != null) etMinPrice.setText(String.valueOf(viewModel.getMinPrice().longValue()));
        if (viewModel.getMaxPrice() != null) etMaxPrice.setText(String.valueOf(viewModel.getMaxPrice().longValue()));
        if (viewModel.getMinRating() != null) sliderRating.setValue(viewModel.getMinRating().floatValue());
        if (viewModel.getInStock() != null) switchInStock.setChecked(viewModel.getInStock());

        // Apply
        btnApply.setOnClickListener(v -> {
            String brandText = etBrand.getText() != null ? etBrand.getText().toString().trim() : "";
            viewModel.setBrand(brandText.isEmpty() ? null : brandText);

            String minPriceStr = etMinPrice.getText() != null ? etMinPrice.getText().toString().trim() : "";
            viewModel.setMinPrice(minPriceStr.isEmpty() ? null : Double.parseDouble(minPriceStr));

            String maxPriceStr = etMaxPrice.getText() != null ? etMaxPrice.getText().toString().trim() : "";
            viewModel.setMaxPrice(maxPriceStr.isEmpty() ? null : Double.parseDouble(maxPriceStr));

            float rating = sliderRating.getValue();
            viewModel.setMinRating(rating > 0 ? (double) rating : null);

            viewModel.setInStock(switchInStock.isChecked() ? true : null);

            viewModel.applyFilters();
            dialog.dismiss();
        });

        // Reset
        btnReset.setOnClickListener(v -> {
            etBrand.setText("");
            etMinPrice.setText("");
            etMaxPrice.setText("");
            sliderRating.setValue(0);
            switchInStock.setChecked(false);

            viewModel.clearFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    // ===== Product Detail =====
    private void openProductDetail(Map<String, Object> product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        String productId = "";
        Object idObj = product.get("_id");
        if (idObj instanceof String) {
            productId = (String) idObj;
        }
        intent.putExtra("product_id", productId);

        Object nameObj = product.get("name");
        if (nameObj instanceof String) {
            intent.putExtra("product_name", (String) nameObj);
        }
        startActivity(intent);
    }
}
