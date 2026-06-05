package com.aura.pc.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.databinding.ActivityAuraProductsBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListActivity extends BaseActivity<ActivityAuraProductsBinding> {
    private int visibleProductCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.aura_orange));
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);
        setupActions();
        setupProductCards();
        setupLoadMoreButton();
    }

    @Override
    protected ActivityAuraProductsBinding inflateBinding() {
        return ActivityAuraProductsBinding.inflate(getLayoutInflater());
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

    private void setupProductCards() {
        visibleProductCount = binding.productGrid.getChildCount();
        for (int i = 0; i < visibleProductCount; i++) {
            View card = binding.productGrid.getChildAt(i);
            card.setOnClickListener(v -> openProductDetail());
            bindCartButtons(card);
        }
    }

    private void setupLoadMoreButton() {
        updateLoadMoreButton(visibleProductCount);
        binding.loadMoreProductsButton.setOnClickListener(v -> refreshProductTotal());
        refreshProductTotal();
    }

    private void refreshProductTotal() {
        int requestLimit = Math.max(visibleProductCount, 1);
        ApiClient.getInstance(this)
                .getApiService()
                .getProductsPaginatedMap(1, requestLimit)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        Map<String, Object> body = response.body();
                        int totalProducts = getTotalProductCount(body);
                        if (response.isSuccessful() && totalProducts > 0) {
                            int remainingProducts = Math.max(totalProducts - visibleProductCount, 0);
                            updateLoadMoreButton(remainingProducts);
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        if (!isFinishing() && !isDestroyed()) {
                            updateLoadMoreButton(visibleProductCount);
                        }
                    }
                });
    }

    private int getTotalProductCount(@Nullable Map<String, Object> responseBody) {
        if (responseBody == null) {
            return 0;
        }
        Object total = responseBody.get("total");
        if (total instanceof Number) {
            return ((Number) total).intValue();
        }
        if (total instanceof String) {
            try {
                return Integer.parseInt((String) total);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void updateLoadMoreButton(int productCount) {
        String text = getString(R.string.product_list_load_more, productCount);
        binding.loadMoreProductsButton.setText(text);
        binding.loadMoreProductsButton.setContentDescription(text);
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
}
