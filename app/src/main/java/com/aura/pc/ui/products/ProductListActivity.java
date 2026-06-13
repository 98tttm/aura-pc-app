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
import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.databinding.ActivityAuraProductsBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;
import com.example.aura_pc_app.utils.ProductSearchUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

/**
 * Màn hình danh sách sản phẩm với manual Paging, Filter (BottomSheet), Sort (Spinner),
 * và Category Chips chuyển đổi nhanh.
 */
public class ProductListActivity extends BaseActivity<ActivityAuraProductsBinding> {

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

    // Category chip mapping
    private static final int[] CHIP_IDS = {
            R.id.chipAll, R.id.chipPC, R.id.chipLaptop, R.id.chipMonitor, R.id.chipAccessories
    };
    private static final String[] CHIP_SLUGS = {
            null, "pc", "laptop", "man-hinh", "linh-kien"
    };
    
    private boolean isProgrammaticChange = false;

    @Override
    protected ActivityAuraProductsBinding inflateBinding() {
        return ActivityAuraProductsBinding.inflate(getLayoutInflater());
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
        if (initialQuery != null && !initialQuery.trim().isEmpty()) {
            binding.searchInput.setText(initialQuery.trim());
            binding.searchInput.setSelection(binding.searchInput.getText().length());
            viewModel.setSearch(initialQuery);
            viewModel.loadFirstPage();
        } else if (initialCategory != null) {
            selectCategoryChip(initialCategory);
        } else {
            viewModel.loadFirstPage();
        }
    }

    // ===== Search input (tap → open ProductSearchActivity) =====
    private void initSearchInput() {
        binding.searchInput.setFocusable(false);
        binding.searchInput.setFocusableInTouchMode(false);
        binding.searchInput.setOnClickListener(v -> openProductSearch());
    }

    @SuppressWarnings("deprecation")
    private void openProductSearch() {
        Intent intent = new Intent(this, ProductSearchActivity.class);
        intent.putExtra(ProductSearchActivity.EXTRA_SOURCE, ProductSearchActivity.SOURCE_PRODUCT_LIST);
        startActivityForResult(intent, REQUEST_SEARCH);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SEARCH || resultCode != Activity.RESULT_OK || data == null) return;
        String keyword = data.getStringExtra("query");
        String category = data.getStringExtra("category");
        if (keyword != null && !keyword.isEmpty()) {
            binding.searchInput.setText(keyword);
            runKeywordSearch(keyword);
        } else if (category != null && !category.isEmpty()) {
            clearKeyword();
            selectCategoryChip(category);
        }
    }

    private void runKeywordSearch(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        viewModel.setCategory(null);
        isProgrammaticChange = true;
        binding.chipGroupCategory.clearCheck();
        binding.chipAll.setChecked(true);
        isProgrammaticChange = false;
        // Backend phân biệt dấu -> khôi phục dấu cho keyword trước khi gửi.
        viewModel.setSearch(ProductSearchUtils.restoreDiacritics(kw));
        viewModel.applyFilters();
    }

    private void clearKeyword() {
        viewModel.setSearch(null);
        if (binding.searchInput.getText().length() > 0) {
            binding.searchInput.setText("");
        }
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
                Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
        );
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
        ChipGroup chipGroup = binding.chipGroupCategory;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (isProgrammaticChange) return;

            if (checkedIds.isEmpty()) {
                isProgrammaticChange = true;
                binding.chipAll.setChecked(true);
                isProgrammaticChange = false;

                clearKeyword();
                viewModel.setCategory(null);
                viewModel.applyFilters();
                return;
            }

            int checkedId = checkedIds.get(0);
            for (int i = 0; i < CHIP_IDS.length; i++) {
                if (CHIP_IDS[i] == checkedId) {
                    clearKeyword();
                    viewModel.setCategory(CHIP_SLUGS[i]);
                    viewModel.applyFilters();
                    break;
                }
            }
        });
    }

    private void selectCategoryChip(String categorySlug) {
        viewModel.setCategory(categorySlug);
        
        isProgrammaticChange = true;
        boolean found = false;
        for (int i = 0; i < CHIP_SLUGS.length; i++) {
            if (categorySlug.equals(CHIP_SLUGS[i])) {
                binding.chipGroupCategory.check(CHIP_IDS[i]);
                found = true;
                break;
            }
        }
        
        if (!found) {
            binding.chipGroupCategory.clearCheck();
        }
        isProgrammaticChange = false;

        viewModel.loadFirstPage();
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
        Intent intent = new Intent(this, MainActivity.class);
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
