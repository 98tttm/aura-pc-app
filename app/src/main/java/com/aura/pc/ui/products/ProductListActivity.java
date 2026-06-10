package com.aura.pc.ui.products;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.adapter.ProductListAdapter;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.utils.LocaleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình danh sách sản phẩm theo danh mục.
 * Nhận category slug và tên danh mục từ Intent, gọi API lọc theo ?category=slug.
 * Hỗ trợ phân trang: hiển thị 20 sản phẩm mỗi lần, bấm "Xem thêm" để tải tiếp.
 */
public class ProductListActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_SLUG = "extra_category_slug";
    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";

    private RecyclerView rvProducts;
    private ProgressBar progressBar;
    private TextView tvCategoryTitle, tvProductCount, tvError, tvEmpty, btnRetry;
    private View layoutError;

    // Pagination views
    private View layoutLoadMore;
    private TextView btnLoadMore;
    private ProgressBar progressLoadMore;

    // Pagination state
    private int currentPage = 1;
    private static final int PAGE_LIMIT = 20;
    private int totalProducts = 0;
    private boolean isLoading = false;
    private final List<Map<String, Object>> productList = new ArrayList<>();
    private ProductListAdapter adapter;

    private String categorySlug;
    private String categoryName;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // Nhận dữ liệu từ Intent
        categorySlug = getIntent().getStringExtra(EXTRA_CATEGORY_SLUG);
        categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        if (categorySlug == null || categorySlug.isEmpty()) {
            finish();
            return;
        }

        initViews();
        loadProducts(1);
    }

    private void initViews() {
        rvProducts = findViewById(R.id.rvProducts);
        progressBar = findViewById(R.id.progressBar);
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle);
        tvProductCount = findViewById(R.id.tvProductCount);
        tvError = findViewById(R.id.tvError);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnRetry = findViewById(R.id.btnRetry);
        layoutError = findViewById(R.id.layoutError);

        // Pagination views
        layoutLoadMore = findViewById(R.id.layoutLoadMore);
        btnLoadMore = findViewById(R.id.btnLoadMore);
        progressLoadMore = findViewById(R.id.progressLoadMore);

        ImageButton btnBack = findViewById(R.id.btnBack);

        // Hiển thị tên danh mục trên thanh tiêu đề
        if (categoryName != null && !categoryName.isEmpty()) {
            tvCategoryTitle.setText(categoryName);
        } else {
            tvCategoryTitle.setText(categorySlug);
        }

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Nút thử lại khi lỗi
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                currentPage = 1;
                productList.clear();
                loadProducts(1);
            });
        }

        // Nút "Xem thêm"
        if (btnLoadMore != null) {
            btnLoadMore.setOnClickListener(v -> loadNextPage());
        }

        // RecyclerView grid 2 cột — khởi tạo adapter 1 lần duy nhất
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ProductListAdapter(productList);
        adapter.setOnProductClickListener(this::onProductClicked);
        rvProducts.setAdapter(adapter);
    }

    private void onProductClicked(Map<String, Object> product) {
        // Mở màn hình chi tiết sản phẩm (MainActivity)
        Intent intent = new Intent(ProductListActivity.this, MainActivity.class);

        // Backend API getProductById yêu cầu truyền MongoDB _id
        String productId = "";
        Object mongoId = product.get("_id");
        if (mongoId instanceof String) {
            productId = (String) mongoId;
        } else {
            Object idObj = product.get("product_id");
            if (idObj instanceof String) {
                productId = (String) idObj;
            }
        }
        intent.putExtra("product_id", productId);

        // Truyền thêm tên để hiển thị nhanh
        Object nameObj = product.get("name");
        if (nameObj instanceof String) {
            intent.putExtra("product_name", (String) nameObj);
        }

        startActivity(intent);
    }

    private void loadNextPage() {
        if (isLoading) return;
        loadProducts(currentPage + 1);
    }

    private void loadProducts(int page) {
        if (isLoading) return;
        isLoading = true;

        if (page == 1) {
            // Trang đầu tiên: hiển thị loading lớn ở giữa màn hình
            showLoading();
            productList.clear();
            adapter.notifyDataSetChanged();
        } else {
            // Trang tiếp theo: hiển thị loading nhỏ ở cuối, ẩn nút "Xem thêm"
            if (btnLoadMore != null) btnLoadMore.setVisibility(View.GONE);
            if (progressLoadMore != null) progressLoadMore.setVisibility(View.VISIBLE);
        }

        ApiClient.getInstance(this).getApiService()
                .getProductsByCategory(categorySlug, page, PAGE_LIMIT)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        isLoading = false;
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                Map<String, Object> body = response.body();
                                List<Map<String, Object>> items =
                                        (List<Map<String, Object>>) body.get("items");

                                // Lấy tổng số sản phẩm từ server
                                Object totalObj = body.get("total");
                                if (totalObj instanceof Number) {
                                    totalProducts = ((Number) totalObj).intValue();
                                }

                                if (items != null && !items.isEmpty()) {
                                    currentPage = page;
                                    int insertStart = productList.size();
                                    productList.addAll(items);
                                    
                                    if (page == 1) {
                                        adapter.notifyDataSetChanged();
                                    } else {
                                        adapter.notifyItemRangeInserted(insertStart, items.size());
                                    }

                                    showProducts();
                                } else if (page == 1) {
                                    showEmpty();
                                } else {
                                    // Không còn sản phẩm để tải
                                    hideLoadMore();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (page == 1) {
                                    showError(getString(R.string.product_list_error));
                                } else {
                                    // Lỗi parsing ở trang > 1, hiện lại nút Xem thêm
                                    showLoadMoreButton();
                                }
                            }
                        } else {
                            if (page == 1) {
                                showError(getString(R.string.product_list_error));
                            } else {
                                showLoadMoreButton();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        isLoading = false;
                        t.printStackTrace();
                        if (page == 1) {
                            showError(getString(R.string.product_list_network_error));
                        } else {
                            showLoadMoreButton();
                        }
                    }
                });
    }

    private void showProducts() {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        rvProducts.setVisibility(View.VISIBLE);

        // Hiển thị số lượng sản phẩm đang hiện / tổng
        tvProductCount.setText(getString(R.string.product_list_count, productList.size()));

        // Kiểm tra có còn sản phẩm để tải không
        if (productList.size() >= totalProducts) {
            hideLoadMore();
        } else {
            showLoadMoreButton();
        }
    }

    private void showLoadMoreButton() {
        if (layoutLoadMore != null) layoutLoadMore.setVisibility(View.VISIBLE);
        if (btnLoadMore != null) btnLoadMore.setVisibility(View.VISIBLE);
        if (progressLoadMore != null) progressLoadMore.setVisibility(View.GONE);
    }

    private void hideLoadMore() {
        if (layoutLoadMore != null) layoutLoadMore.setVisibility(View.GONE);
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvProducts.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        hideLoadMore();
        tvProductCount.setText("");
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        rvProducts.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        tvError.setText(message);
        hideLoadMore();
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        rvProducts.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvProductCount.setText(getString(R.string.product_list_count, 0));
        hideLoadMore();
    }
}
