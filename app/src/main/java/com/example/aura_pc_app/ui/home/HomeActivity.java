package com.example.aura_pc_app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.aura.pc.ui.cart.CartActivity;
import com.aura.pc.ui.categories.CategoriesActivity;
import com.aura.pc.ui.products.ProductListActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.adapter.HomeProductAdapter;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.databinding.ActivityHomeBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;

public class HomeActivity extends BaseActivity<ActivityHomeBinding> {
    private HomeProductAdapter productAdapter;
    private HomeViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_HOME);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        setupProductList();
        setupHomeActions();
        observeProducts();
    }

    @Override
    protected ActivityHomeBinding inflateBinding() {
        return ActivityHomeBinding.inflate(getLayoutInflater());
    }

    private void setupHomeActions() {
        binding.topCartButton.setOnClickListener(v -> openCart());
        binding.topNotificationsButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_notifications_pending, Toast.LENGTH_SHORT).show());
        binding.topMenuButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_menu_pending, Toast.LENGTH_SHORT).show());
        binding.searchFilterButton.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));
        binding.contextualFab.setOnClickListener(v ->
                Toast.makeText(this, R.string.msg_chat_pending, Toast.LENGTH_SHORT).show());
        binding.homeHeroCard.setOnClickListener(v -> openProductDetail());
        binding.homeHeroCta.setOnClickListener(v -> openProductDetail());
        binding.homeQuickCategories.setOnClickListener(v ->
                startActivity(new Intent(this, CategoriesActivity.class)));
        binding.homeQuickBuilder.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_builder_pending, Toast.LENGTH_SHORT).show());
        binding.homeViewAllProducts.setOnClickListener(v -> openProductList());
        setupCategoryStrip();
    }

    private void setupProductList() {
        productAdapter = new HomeProductAdapter(new HomeProductAdapter.ProductClickListener() {
            @Override
            public void onProductClick(ProductEntity product) {
                openProductDetail();
            }

            @Override
            public void onCartClick(ProductEntity product) {
                addProductToCart(product);
            }
        });
        binding.productRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.productRecyclerView.setAdapter(productAdapter);
        binding.productRecyclerView.setHasFixedSize(false);
    }

    private void observeProducts() {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        viewModel.getProducts().observe(this, products -> {
            productAdapter.setProducts(products);
            binding.loadingProgress.setVisibility(View.GONE);
        });
        viewModel.getProductAdded().observe(this, added -> {
            if (Boolean.TRUE.equals(added)) {
                Toast.makeText(this, R.string.cart_added, Toast.LENGTH_SHORT).show();
                viewModel.clearProductAdded();
            }
        });
        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCategoryStrip() {
        int[] titles = {
                R.string.category_laptop,
                R.string.home_category_gaming_pc,
                R.string.category_monitor,
                R.string.home_category_keyboard,
                R.string.home_category_mouse,
                R.string.home_category_audio
        };
        int[] icons = {
                R.drawable.figma_cat_laptop,
                R.drawable.figma_cat_gaming_pc,
                R.drawable.figma_cat_monitor,
                R.drawable.figma_cat_keyboard,
                R.drawable.figma_product_mouse,
                R.drawable.ic_categories
        };

        int count = Math.min(binding.homeCategoryStrip.getChildCount(), titles.length);
        for (int i = 0; i < count; i++) {
            View item = binding.homeCategoryStrip.getChildAt(i);
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

    private void openProductDetail() {
        startActivity(new Intent(this, MainActivity.class));
    }

    private void openProductList() {
        startActivity(new Intent(this, ProductListActivity.class));
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private void addProductToCart(ProductEntity product) {
        viewModel.addProductToCart(product);
    }
}
