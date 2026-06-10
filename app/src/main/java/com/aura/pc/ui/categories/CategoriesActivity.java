package com.aura.pc.ui.categories;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.pc.ui.products.ProductListActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.adapter.CategoryAdapter;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.domain.model.Category;
import com.example.aura_pc_app.utils.LocaleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView categoriesRecyclerView;
    private ProgressBar progressBar;
    private TextView tvError;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        initViews();
        loadCategories();

        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);
    }

    private void initViews() {
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        progressBar = findViewById(R.id.progressBarCategories);
        tvError = findViewById(R.id.tvCategoriesError);

        // Grid 2 cột
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void loadCategories() {
        showLoading(true);

        ApiClient.getInstance(this).getApiService().getCategories()
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call,
                                           Response<List<Map<String, Object>>> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<Category> categories = parseCategories(response.body());
                            if (categories.isEmpty()) {
                                showError("Không có danh mục nào.");
                            } else {
                                bindCategories(categories);
                            }
                        } else {
                            showError("Không thể tải danh mục. Vui lòng thử lại.");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        showLoading(false);
                        showError("Lỗi kết nối mạng. Vui lòng kiểm tra lại.");
                    }
                });
    }

    /**
     * Chỉ giữ lại danh mục cấp 1 (level == 1) để hiển thị trên màn hình chính.
     */
    private List<Category> parseCategories(List<Map<String, Object>> rawList) {
        List<Category> result = new ArrayList<>();
        for (Map<String, Object> item : rawList) {
            int level = 0;
            Object lvl = item.get("level");
            if (lvl instanceof Number) {
                level = ((Number) lvl).intValue();
            }
            if (level != 1) continue; // Chỉ lấy danh mục cấp 1

            String categoryId = getStr(item, "category_id");
            String name       = getStr(item, "name");
            String slug       = getStr(item, "slug");
            String parentId   = getStr(item, "parent_id");

            result.add(new Category(categoryId, name, slug, parentId, level));
        }
        return result;
    }

    private void bindCategories(List<Category> categories) {
        CategoryAdapter adapter = new CategoryAdapter(categories);
        adapter.setOnCategoryClickListener(category -> {
            Intent intent = new Intent(CategoriesActivity.this, ProductListActivity.class);
            intent.putExtra(ProductListActivity.EXTRA_CATEGORY_SLUG, category.getSlug());
            intent.putExtra(ProductListActivity.EXTRA_CATEGORY_NAME, category.getName());
            startActivity(intent);
        });
        categoriesRecyclerView.setAdapter(adapter);
        categoriesRecyclerView.setVisibility(View.VISIBLE);
        if (tvError != null) tvError.setVisibility(View.GONE);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (categoriesRecyclerView != null && isLoading) {
            categoriesRecyclerView.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        if (tvError != null) {
            tvError.setText(msg);
            tvError.setVisibility(View.VISIBLE);
        }
        if (categoriesRecyclerView != null) {
            categoriesRecyclerView.setVisibility(View.GONE);
        }
    }

    private static String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : "";
    }
}
