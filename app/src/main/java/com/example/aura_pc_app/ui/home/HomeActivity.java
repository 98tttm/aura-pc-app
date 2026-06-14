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
import com.aura.pc.ui.productdetail.ProductDetailActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.adapter.HomeProductAdapter;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.databinding.ActivityHomeBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;
import com.example.aura_pc_app.utils.AuthGate;
import java.util.ArrayList;
import java.util.List;

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
                addProductToCart();
            }
        });
        binding.productRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.productRecyclerView.setAdapter(productAdapter);
        binding.productRecyclerView.setHasFixedSize(false);
    }

    private void observeProducts() {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        viewModel.getProducts().observe(this, products -> {
            List<ProductEntity> displayProducts = products == null || products.isEmpty()
                    ? createFallbackProducts()
                    : products;
            productAdapter.setProducts(displayProducts);
            binding.loadingProgress.setVisibility(View.GONE);
        });
    }

    private void setupCategoryStrip() {
        String[] titles = {
                "Laptop", "PC", "Màn hình", "Linh kiện", "Khác", "Xem tất cả"
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
            String slug = null;
            if (i == 0) slug = "laptop";
            else if (i == 1) slug = "pc";
            else if (i == 2) slug = "man-hinh";
            else if (i == 3) slug = "linh-kien";

            final String finalSlug = slug;
            item.setOnClickListener(v -> {
                if (finalSlug == null) {
                    startActivity(new Intent(this, CategoriesActivity.class));
                } else {
                    Intent intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra("category", finalSlug);
                    startActivity(intent);
                }
            });
        }
    }

    private void openProductDetail() {
        startActivity(new Intent(this, ProductDetailActivity.class));
    }

    private void openProductList() {
        startActivity(new Intent(this, ProductListActivity.class));
    }

    private void openCart() {
        startActivity(new Intent(this, CartActivity.class));
    }

    private void addProductToCart() {
        if (!AuthGate.requireLogin(this, CartActivity.class)) {
            return;
        }
        openCart();
    }

    private List<ProductEntity> createFallbackProducts() {
        List<ProductEntity> products = new ArrayList<>();
        addFallback(products, "fallback-acer-nitro", "Laptop Gaming Acer Nitro V 16S", 28990000, 24590000);
        addFallback(products, "fallback-aura-obsidian", "Aura Obsidian Pro X", 100500000, 85000000);
        addFallback(products, "fallback-pc-ai", "Aura PC AI Creator", 52990000, 48990000);
        addFallback(products, "fallback-monitor", "Màn hình Gaming 27 inch", 7990000, 6490000);
        addFallback(products, "fallback-keyboard", "Bàn phím cơ Aura RGB", 1890000, 1490000);
        addFallback(products, "fallback-ssd", "SSD NVMe Gen4 1TB", 2490000, 1990000);
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
}
